package prisons.solar.npclib.core.event;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.npc.NPCDespawnEvent;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.viewer.Viewer;

import java.time.Instant;

/**
 * Simple implementation of {@link NPCDespawnEvent}.
 */
public class SimpleNPCDespawnEvent implements NPCDespawnEvent {

    private final NPC<?> npc;
    private final Viewer viewer;
    private final Instant timestamp;

    public SimpleNPCDespawnEvent(@NotNull NPC<?> npc, @NotNull Viewer viewer) {
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
    public @NotNull Instant timestamp() {
        return timestamp;
    }
}
