package prisons.solar.npclib.api.event.npc.health;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.Cancellable;
import prisons.solar.npclib.api.event.npc.NPCEvent;
import prisons.solar.npclib.api.health.HealthComponent;
import prisons.solar.npclib.api.npc.NPC;

import java.time.Instant;

public class DeathEvent implements NPCEvent, Cancellable {
    private final NPC<?> npc;
    private final HealthComponent.DamageSource source;
    private final Instant timestamp;
    private boolean cancelled = false;

    public DeathEvent(NPC<?> npc, HealthComponent.DamageSource source, Instant timestamp) {
        this.npc = npc;
        this.source = source;
        this.timestamp = timestamp;
    }

    public NPC<?> getNpc() {
        return npc;
    }

    public HealthComponent.DamageSource getSource() {
        return source;
    }

    public Instant getTimestamp() {
        return timestamp;
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
    public @NotNull NPC<?> npc() {
        return npc;
    }

    @Override
    public @NotNull Instant timestamp() {
        return timestamp;
    }
}
