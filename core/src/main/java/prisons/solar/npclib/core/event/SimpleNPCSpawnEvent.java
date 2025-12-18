package prisons.solar.npclib.core.event;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.NPCSpawnEvent;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.viewer.Viewer;

/**
 * Simple implementation of {@link NPCSpawnEvent}.
 */
public class SimpleNPCSpawnEvent implements NPCSpawnEvent {

    private final NPC<?> npc;
    private final Viewer viewer;
    private boolean cancelled;

    public SimpleNPCSpawnEvent(@NotNull NPC<?> npc, @NotNull Viewer viewer) {
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
