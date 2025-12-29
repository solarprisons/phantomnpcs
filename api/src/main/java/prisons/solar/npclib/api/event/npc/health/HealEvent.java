package prisons.solar.npclib.api.event.npc.health;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.Cancellable;
import prisons.solar.npclib.api.event.npc.NPCEvent;
import prisons.solar.npclib.api.npc.NPC;

import java.time.Instant;

public class HealEvent implements NPCEvent, Cancellable {
    private final NPC<?> npc;
    private float healAmount;
    private boolean cancelled = false;
    private final Instant timestamp;

    public HealEvent(NPC<?> npc, float healAmount) {
        this.npc = npc;
        this.healAmount = healAmount;
        this.timestamp = Instant.now();
    }

    public NPC<?> getNpc() {
        return npc;
    }

    public float getHealAmount() {
        return healAmount;
    }

    public void setHealAmount(float healAmount) {
        this.healAmount = healAmount;
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
