package prisons.solar.npclib.api.viewer;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;

/**
 * A rule that determines whether a viewer can see an NPC.
 */
@FunctionalInterface
public interface VisibilityRule {

    /**
     * Tests whether a viewer should be able to see an NPC.
     *
     * @param viewer the viewer
     * @param npc    the NPC
     * @return true if the NPC should be visible
     */
    boolean test(@NotNull Viewer viewer, @NotNull NPC<?> npc);

    /**
     * Combines this rule with another using AND logic.
     *
     * @param other the other rule
     * @return combined rule
     */
    default VisibilityRule and(@NotNull VisibilityRule other) {
        return (viewer, npc) -> this.test(viewer, npc) && other.test(viewer, npc);
    }

    /**
     * Combines this rule with another using OR logic.
     *
     * @param other the other rule
     * @return combined rule
     */
    default VisibilityRule or(@NotNull VisibilityRule other) {
        return (viewer, npc) -> this.test(viewer, npc) || other.test(viewer, npc);
    }

    /**
     * Negates this rule.
     *
     * @return negated rule
     */
    default VisibilityRule negate() {
        return (viewer, npc) -> !this.test(viewer, npc);
    }

    /**
     * A rule that always returns true.
     *
     * @return always-true rule
     */
    static VisibilityRule always() {
        return (viewer, npc) -> true;
    }

    /**
     * A rule that always returns false.
     *
     * @return always-false rule
     */
    static VisibilityRule never() {
        return (viewer, npc) -> false;
    }

    /**
     * Creates a distance-based visibility rule.
     *
     * @param maxDistance maximum view distance
     * @return distance rule
     */
    static VisibilityRule withinDistance(double maxDistance) {
        double maxDistSq = maxDistance * maxDistance;
        return (viewer, npc) -> viewer.position().distanceSquared(npc.position()) <= maxDistSq;
    }

    /**
     * Creates a permission-based visibility rule.
     *
     * @param permission the required permission
     * @return permission rule
     */
    static VisibilityRule withPermission(@NotNull String permission) {
        return (viewer, npc) -> viewer.hasPermission(permission);
    }

    /**
     * Creates a world-based visibility rule.
     *
     * @param worldId the required world
     * @return world rule
     */
    static VisibilityRule inWorld(@NotNull String worldId) {
        return (viewer, npc) -> viewer.position().worldId().equals(worldId)
                && npc.position().worldId().equals(worldId);
    }
}
