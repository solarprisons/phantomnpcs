package prisons.solar.npclib.api.ai.pathfinding;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.Position;

import java.util.List;

/**
 * Simple immutable path implementation.
 * Stores waypoints and pre-calculates path length for efficient access.
 */
public class SimplePath implements Path {

    private final List<Position> waypoints;
    private final double length;

    public SimplePath(@NotNull List<Position> waypoints) {
        this.waypoints = List.copyOf(waypoints);
        this.length = calculateLength(waypoints);
    }

    @Override
    public @NotNull List<Position> waypoints() {
        return waypoints;
    }

    @Override
    public double length() {
        return length;
    }

    @Override
    public @NotNull Position start() {
        if (waypoints.isEmpty()) {
            throw new IllegalStateException("Path has no waypoints");
        }
        return waypoints.getFirst();
    }

    @Override
    public @NotNull Position goal() {
        if (waypoints.isEmpty()) {
            throw new IllegalStateException("Path has no waypoints");
        }
        return waypoints.getLast();
    }

    private double calculateLength(List<Position> waypoints) {
        if (waypoints.size() < 2) {
            return 0.0;
        }

        double total = 0.0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            total += waypoints.get(i).distance(waypoints.get(i + 1));
        }
        return total;
    }
}
