package prisons.solar.npclib.core.event;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.NPCDespawnEvent;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.viewer.Viewer;

/**
 * Simple implementation of {@link NPCDespawnEvent}.
 */
public class SimpleNPCDespawnEvent implements NPCDespawnEvent {

    private final NPC<?> npc;
    private final Viewer viewer;

    public SimpleNPCDespawnEvent(@NotNull NPC<?> npc, @NotNull Viewer viewer) {
        this.npc = npc;
        this.viewer = viewer;
    }

    @Override
    public @NotNull NPC<?> npc() {
        return npc;
    }

    @Override
    public @NotNull Viewer viewer() {
        return viewer;
    }
}
