package prisons.solar.npclib.api.event;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.interaction.InteractionHandler;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.viewer.Viewer;

/**
 * Event fired when a viewer interacts with an NPC.
 */
public interface NPCInteractEvent extends NPCEvent, Cancellable {

    /**
     * Gets the viewer who interacted.
     *
     * @return the viewer
     */
    @NotNull Viewer viewer();

    /**
     * Gets the type of click.
     *
     * @return the click type
     */
    @NotNull InteractionHandler.ClickType clickType();

    /**
     * Gets the hand used for the interaction.
     *
     * @return the hand
     */
    @NotNull InteractionHandler.Hand hand();
}
