package prisons.solar.npclib.core.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.ai.PathNavigator;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple straight-line path navigator.
 * Moves NPCs directly toward targets without collision detection.
 * For more sophisticated pathfinding, use a platform-specific implementation
 * that can access world data for collision and A* pathfinding.
 */
public class SimplePathNavigator implements PathNavigator {

    private final NPC<?> npc;
    private Position target;
    private List<Position> path;
    private double speed = 0.1; // blocks per tick
    private boolean navigating = false;

    private static final double ARRIVAL_THRESHOLD = 0.5;
    private static final double ARRIVAL_THRESHOLD_SQ = ARRIVAL_THRESHOLD * ARRIVAL_THRESHOLD;

    public SimplePathNavigator(@NotNull NPC<?> npc) {
        this.npc = npc;
    }

    @Override
    public boolean navigateTo(@NotNull Position target) {
        return navigateTo(target, 1.0);
    }

    @Override
    public boolean navigateTo(@NotNull Position target, double speedMultiplier) {
        // Can't navigate to different worlds
        if (!target.worldId().equals(npc.position().worldId())) {
            return false;
        }

        this.target = target;
        this.path = computePath(npc.position(), target);
        this.navigating = true;
        return true;
    }

    @Override
    public void stop() {
        this.target = null;
        this.path = null;
        this.navigating = false;
    }

    @Override
    public boolean isNavigating() {
        return navigating;
    }

    @Override
    public @Nullable Position target() {
        return target;
    }

    @Override
    public @Nullable List<Position> path() {
        return path != null ? new ArrayList<>(path) : null;
    }

    @Override
    public double speed() {
        return speed;
    }

    @Override
    public void setSpeed(double speed) {
        this.speed = Math.max(0.01, speed);
    }

    @Override
    public void tick() {
        if (!navigating || target == null) {
            return;
        }

        Position current = npc.position();

        // Check if arrived
        if (current.distanceSquared(target) <= ARRIVAL_THRESHOLD_SQ) {
            stop();
            return;
        }

        // Calculate direction
        double dx = target.x() - current.x();
        double dy = target.y() - current.y();
        double dz = target.z() - current.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 0.001) {
            stop();
            return;
        }

        // Normalize and scale by speed
        double moveX = (dx / distance) * speed;
        double moveY = (dy / distance) * speed;
        double moveZ = (dz / distance) * speed;

        // Calculate new position
        double newX = current.x() + moveX;
        double newY = current.y() + moveY;
        double newZ = current.z() + moveZ;

        // Calculate yaw to face movement direction
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

        // Move the NPC
        Position newPosition = Position.of(
                current.worldId(),
                newX, newY, newZ,
                yaw, pitch
        );

        npc.teleport(newPosition);

        // Update path progress
        if (path != null && !path.isEmpty()) {
            Position waypoint = path.get(0);
            if (newPosition.distanceSquared(waypoint) <= ARRIVAL_THRESHOLD_SQ) {
                path.remove(0);
            }
        }
    }

    @Override
    public boolean canReach(@NotNull Position target) {
        // Simple implementation - just check same world and reasonable distance
        if (!target.worldId().equals(npc.position().worldId())) {
            return false;
        }

        // Limit to reasonable range (100 blocks)
        return npc.position().distanceSquared(target) <= 10000;
    }

    /**
     * Computes a simple path (straight line with waypoints).
     * Override in subclasses for more sophisticated pathfinding.
     *
     * @param start the start position
     * @param end   the end position
     * @return list of waypoints
     */
    protected List<Position> computePath(@NotNull Position start, @NotNull Position end) {
        List<Position> waypoints = new ArrayList<>();

        double distance = Math.sqrt(start.distanceSquared(end));
        int segments = Math.max(1, (int) (distance / 2.0)); // Waypoint every 2 blocks

        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            double x = start.x() + (end.x() - start.x()) * t;
            double y = start.y() + (end.y() - start.y()) * t;
            double z = start.z() + (end.z() - start.z()) * t;
            waypoints.add(Position.of(start.worldId(), x, y, z, 0, 0));
        }

        return waypoints;
    }
}
