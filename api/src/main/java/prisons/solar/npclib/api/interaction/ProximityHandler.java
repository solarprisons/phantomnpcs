package prisons.solar.npclib.api.interaction;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.viewer.Viewer;

/**
 * Handler for proximity-based NPC interactions.
 */
public interface ProximityHandler {

    /**
     * Called when a viewer enters the proximity radius of an NPC.
     *
     * @param npc    the NPC
     * @param viewer the viewer who entered
     */
    void onEnter(@NotNull NPC<?> npc, @NotNull Viewer viewer);

    /**
     * Called when a viewer exits the proximity radius of an NPC.
     *
     * @param npc    the NPC
     * @param viewer the viewer who exited
     */
    void onExit(@NotNull NPC<?> npc, @NotNull Viewer viewer);

    /**
     * Called while a viewer remains in the proximity radius (each tick).
     *
     * @param npc    the NPC
     * @param viewer the viewer
     */
    default void onStay(@NotNull NPC<?> npc, @NotNull Viewer viewer) {
        // Default no-op
    }
}
