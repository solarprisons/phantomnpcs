package prisons.solar.npclib.paper.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import prisons.solar.npclib.api.ai.pathfinding.Path;
import prisons.solar.npclib.api.ai.pathfinding.PathResult;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.core.ai.goals.NavigateToPositionGoal;

import java.util.List;
import java.util.UUID;

/**
 * Displays pathfinding debug information on the player's actionbar.
 */
public class PathfindingDebugDisplay {

    public static void updateActionbar(Player player, NavigateToPositionGoal navGoal) {
        if (navGoal == null) {
            player.sendActionBar(Component.text("No active pathfinding", NamedTextColor.GRAY));
            return;
        }

        // Build status display
        Component statusComponent;

        if (navGoal.isPendingPath()) {
            // Show real-time computation progress
            long elapsed = System.currentTimeMillis() - navGoal.getPathRequestStartTime();
            statusComponent = Component.text("⏳ Computing path... ", NamedTextColor.YELLOW)
                .append(Component.text("(" + elapsed + "ms)", NamedTextColor.GRAY));

        } else if (navGoal.getCurrentPath() != null) {
            // Show navigation progress with path stats
            int total = navGoal.getCurrentPath().waypoints().size();
            int current = navGoal.getCurrentWaypointIndex();
            int remaining = total - current;
            double length = navGoal.getCurrentPath().length();

            PathResult lastResult = navGoal.getLastPathResult();
            String statsInfo = "";
            if (lastResult != null) {
                statsInfo = String.format(" [%dms, %d nodes]",
                    lastResult.computeTimeMs(), lastResult.nodesExplored());
            }

            statusComponent = Component.text("▶ Navigating ", NamedTextColor.GREEN)
                .append(Component.text("[" + current + "/" + total + "] ", NamedTextColor.WHITE))
                .append(Component.text(String.format("%.1fm", length), NamedTextColor.AQUA))
                .append(Component.text(statsInfo, NamedTextColor.DARK_GRAY));

        } else {
            // Show failure reason with metrics
            PathResult lastResult = navGoal.getLastPathResult();
            if (lastResult != null && lastResult.status() != PathResult.Status.SUCCESS) {
                String failureInfo = String.format(" [%s: %dms, %d nodes]",
                    lastResult.status(), lastResult.computeTimeMs(), lastResult.nodesExplored());

                statusComponent = Component.text("✗ No path", NamedTextColor.RED)
                    .append(Component.text(failureInfo, NamedTextColor.DARK_RED));
            } else {
                statusComponent = Component.text("✗ No path", NamedTextColor.RED);
            }
        }

        player.sendActionBar(statusComponent);
    }

    /**
     * Displays particles along the path and at key positions.
     * Call this every few ticks to visualize the NPC's path.
     *
     * @param navGoal the navigation goal
     * @param npcPosition current NPC position
     */
    public static void displayPathParticles(NavigateToPositionGoal navGoal, Position npcPosition) {
        if (navGoal == null) return;

        Path path = navGoal.getCurrentPath();
        if (path == null) return;

        World world = Bukkit.getWorld(UUID.fromString(npcPosition.worldId()));
        if (world == null) return;

        List<Position> waypoints = path.waypoints();
        int currentIndex = navGoal.getCurrentWaypointIndex();

        // Spawn particles at each waypoint
        for (int i = 0; i < waypoints.size(); i++) {
            Position wp = waypoints.get(i);
            Location loc = new Location(world, wp.x(), wp.y() + 0.1, wp.z());

            if (i < currentIndex) {
                // Already passed - gray/white
                world.spawnParticle(Particle.DUST, loc, 1,
                    new Particle.DustOptions(org.bukkit.Color.GRAY, 0.5f));
            } else if (i == currentIndex) {
                // Current target - bright green, larger
                world.spawnParticle(Particle.DUST, loc, 3, 0.1, 0.1, 0.1,
                    new Particle.DustOptions(org.bukkit.Color.LIME, 1.5f));
                // Add flame above current waypoint for visibility
                world.spawnParticle(Particle.FLAME, loc.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
            } else {
                // Future waypoints - blue
                world.spawnParticle(Particle.DUST, loc, 1,
                    new Particle.DustOptions(org.bukkit.Color.AQUA, 0.8f));
            }
        }

        // NPC current position - red dust
        Location npcLoc = new Location(world, npcPosition.x(), npcPosition.y() + 0.1, npcPosition.z());
        world.spawnParticle(Particle.DUST, npcLoc, 2, 0.05, 0.05, 0.05,
            new Particle.DustOptions(org.bukkit.Color.RED, 1.0f));

        // Draw line from NPC to current waypoint
        if (currentIndex < waypoints.size()) {
            Position targetWp = waypoints.get(currentIndex);
            drawParticleLine(world, npcPosition, targetWp);
        }
    }

    /**
     * Draws a particle line between two positions.
     */
    private static void drawParticleLine(World world, Position from, Position to) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double dz = to.z() - from.z();
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        int steps = (int) Math.ceil(dist * 4); // 4 particles per block
        if (steps <= 0) return;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = from.x() + dx * t;
            double y = from.y() + dy * t + 0.1;
            double z = from.z() + dz * t;

            Location loc = new Location(world, x, y, z);
            world.spawnParticle(Particle.DUST, loc, 1,
                new Particle.DustOptions(org.bukkit.Color.YELLOW, 0.3f));
        }
    }
}
