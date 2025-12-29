package prisons.solar.npclib.api.event.npc;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;

import java.time.Instant;

/**
 * Base interface for all NPC events.
 */
public interface NPCEvent {
    /**
     * Gets the NPC associated with this event.
     *
     * @return the NPC
     */
    @NotNull NPC<?> npc();
    @NotNull Instant timestamp();
}
