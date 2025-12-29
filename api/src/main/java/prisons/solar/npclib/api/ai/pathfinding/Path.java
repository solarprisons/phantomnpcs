package prisons.solar.npclib.api.ai.pathfinding;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.Position;

import java.util.List;

/**
 * Represents a pathfinding result containing waypoints from start to goal.
 * Implementations should be immutable.
 */
public interface Path {

    /**
     * Returns the list of waypoints from start to goal.
     * @return immutable list of positions
     */
    @NotNull List<Position> waypoints();

    /**
     * Returns the total length of the path.
     * @return path length in blocks
     */
    double length();

    /**
     * Returns the starting position of the path.
     * @return start position
     */
    @NotNull Position start();

    /**
     * Returns the goal position of the path.
     * @return goal position
     */
    @NotNull Position goal();
}
