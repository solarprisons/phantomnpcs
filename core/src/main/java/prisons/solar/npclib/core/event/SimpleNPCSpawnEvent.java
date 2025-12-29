package prisons.solar.npclib.core.event;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.npc.NPCSpawnEvent;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.viewer.Viewer;

import java.time.Instant;

/**
 * Simple implementation of {@link NPCSpawnEvent}.
 */
public class SimpleNPCSpawnEvent implements NPCSpawnEvent {

    private final NPC<?> npc;
    private final Viewer viewer;
    private boolean cancelled;
    private final Instant timestamp;

    public SimpleNPCSpawnEvent(@NotNull NPC<?> npc, @NotNull Viewer viewer) {
        this.npc = npc;
        this.viewer = viewer;
        this.timestamp = Instant.now();
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

    @Override
    public @NotNull Instant timestamp() {
        return timestamp;
    }
}
