package prisons.solar.npclib.api.ai;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.npc.Position;

import java.util.List;

/**
 * Handles pathfinding and movement for NPCs.
 * This is an SPI - implementations can be provided for different pathfinding solutions.
 */
public interface PathNavigator {

    /**
     * Navigates to a target position.
     *
     * @param target the target position
     * @return true if navigation started successfully
     */
    boolean navigateTo(@NotNull Position target);

    /**
     * Navigates to a target position with speed multiplier.
     *
     * @param target the target position
     * @param speed  speed multiplier (1.0 = normal)
     * @return true if navigation started successfully
     */
    boolean navigateTo(@NotNull Position target, double speed);

    /**
     * Stops any current navigation.
     */
    void stop();

    /**
     * Checks if currently navigating.
     *
     * @return true if navigating
     */
    boolean isNavigating();

    /**
     * Gets the current target position.
     *
     * @return the target, or null if not navigating
     */
    @Nullable Position target();

    /**
     * Gets the current path.
     *
     * @return list of positions, or null if no path
     */
    @Nullable List<Position> path();

    /**
     * Gets the current movement speed.
     *
     * @return the speed
     */
    double speed();

    /**
     * Sets the base movement speed.
     *
     * @param speed the speed
     */
    void setSpeed(double speed);

    /**
     * Ticks the navigator, moving the NPC along the path.
     */
    void tick();

    /**
     * Checks if the target is reachable.
     *
     * @param target the target
     * @return true if reachable
     */
    boolean canReach(@NotNull Position target);
}
