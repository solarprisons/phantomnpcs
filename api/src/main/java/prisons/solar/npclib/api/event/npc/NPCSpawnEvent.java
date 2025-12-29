package prisons.solar.npclib.api.event.npc;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.Cancellable;
import prisons.solar.npclib.api.viewer.Viewer;

/**
 * Event fired when an NPC is spawned for a viewer.
 */
public interface NPCSpawnEvent extends NPCEvent, Cancellable {

    /**
     * Gets the viewer the NPC is being spawned for.
     *
     * @return the viewer
     */
    @NotNull Viewer viewer();
}
