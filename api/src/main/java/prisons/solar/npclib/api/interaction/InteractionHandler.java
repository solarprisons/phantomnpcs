package prisons.solar.npclib.api.interaction;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.viewer.Viewer;

/**
 * Handles interactions with NPCs.
 */
@FunctionalInterface
public interface InteractionHandler {

    /**
     * Called when a viewer interacts with an NPC.
     *
     * @param context the interaction context
     */
    void handle(@NotNull Context context);

    /**
     * Context information for an NPC interaction.
     *
     * @param npc       the NPC that was interacted with
     * @param viewer    the viewer who performed the interaction
     * @param clickType the type of click
     * @param hand      the hand used
     */
    record Context(
            @NotNull NPC<?> npc,
            @NotNull Viewer viewer,
            @NotNull ClickType clickType,
            @NotNull Hand hand
    ) {
        /**
         * Returns whether the interaction was a left click (attack).
         *
         * @return true if left click
         */
        public boolean isLeftClick() {
            return clickType == ClickType.LEFT_CLICK;
        }

        /**
         * Returns whether the interaction was a right click (interact).
         *
         * @return true if right click
         */
        public boolean isRightClick() {
            return clickType == ClickType.RIGHT_CLICK;
        }
    }

    /**
     * Hand used in interaction.
     */
    enum Hand {
        MAIN_HAND,
        OFF_HAND
    }

    /**
     * Types of click interactions.
     */
    enum ClickType {
        /**
         * Left click (attack).
         */
        LEFT_CLICK,

        /**
         * Right click (interact).
         */
        RIGHT_CLICK,

        /**
         * Left click while sneaking.
         */
        SHIFT_LEFT_CLICK,

        /**
         * Right click while sneaking.
         */
        SHIFT_RIGHT_CLICK
    }
}
