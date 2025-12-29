package prisons.solar.npclib.api.event.npc;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.viewer.Viewer;

/**
 * Event fired when an NPC is despawned for a viewer.
 */
public interface NPCDespawnEvent extends NPCEvent {

    /**
     * Gets the viewer the NPC is being despawned for.
     *
     * @return the viewer
     */
    @NotNull Viewer viewer();
}
