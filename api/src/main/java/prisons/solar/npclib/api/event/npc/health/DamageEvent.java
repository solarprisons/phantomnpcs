package prisons.solar.npclib.api.event.npc.health;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.Cancellable;
import prisons.solar.npclib.api.event.npc.NPCEvent;
import prisons.solar.npclib.api.health.HealthComponent;
import prisons.solar.npclib.api.npc.NPC;

import java.time.Instant;

public class DamageEvent implements NPCEvent, Cancellable {
    private final NPC<?> npc;
    private final HealthComponent.DamageSource source;
    private float damage;
    private boolean cancelled = false;
    private final Instant timestamp;

    public DamageEvent(NPC<?> npc, HealthComponent.DamageSource source) {
        this.npc = npc;
        this.source = source;
        this.timestamp = Instant.now();
    }

    public NPC<?> getNpc() {
        return npc;
    }

    public HealthComponent.DamageSource getSource() {
        return source;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
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
