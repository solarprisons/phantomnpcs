package prisons.solar.npclib.core.health;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.npc.health.DamageEvent;
import prisons.solar.npclib.api.event.npc.health.DeathEvent;
import prisons.solar.npclib.api.event.npc.health.HealEvent;
import prisons.solar.npclib.api.health.HealthComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Default implementation of {@link HealthComponent}.
 * Provides basic health management with event callbacks.
 */
public class DefaultHealthComponent implements HealthComponent {

    private float health;
    private float maxHealth;
    private boolean dead = false;
    private boolean invulnerable = false;
    private int invulnerableTicks = 0;

    private final List<Consumer<DamageEvent>> damageListeners = new ArrayList<>();
    private final List<Consumer<DeathEvent>> deathListeners = new ArrayList<>();
    private final List<Consumer<HealEvent>> healListeners = new ArrayList<>();

    /**
     * Creates a new health component with the specified max health.
     *
     * @param maxHealth the maximum health
     */
    public DefaultHealthComponent(float maxHealth) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    @Override
    public float health() {
        return health;
    }

    @Override
    public float maxHealth() {
        return maxHealth;
    }

    @Override
    public boolean isDead() {
        return dead;
    }

    @Override
    public boolean isInvulnerable() {
        return invulnerable || invulnerableTicks > 0;
    }

    @Override
    public void setHealth(float health) {
        this.health = Math.max(0, Math.min(health, maxHealth));
        if (this.health <= 0 && !dead) {
            dead = true;
        }
    }

    @Override
    public void setMaxHealth(float maxHealth) {
        this.maxHealth = Math.max(1, maxHealth);
        if (this.health > this.maxHealth) {
            this.health = this.maxHealth;
        }
    }

    @Override
    public void heal(float amount) {
        if (dead || amount <= 0) {
            return;
        }

        float before = health;
        health = Math.min(health + amount, maxHealth);
        float healed = health - before;

        if (healed > 0) {
            healListeners.forEach(l -> l.accept(null));
        }
    }

    @Override
    public DamageResult damage(@NotNull DamageSource source, float amount) {
        if (dead || isInvulnerable() || amount <= 0) {
            return new DamageResult(0, health, health, false, isInvulnerable());
        }

        float before = health;
        health = Math.max(0, health - amount);
        boolean killed = health <= 0;

        if (killed) {
            dead = true;
        }

        return new DamageResult(amount, before, health, killed, false);
    }

    @Override
    public void kill() {
        health = 0;
        dead = true;
    }

    @Override
    public void revive() {
        dead = false;
        health = maxHealth;
    }

    @Override
    public void setInvulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    @Override
    public void setInvulnerableTicks(int ticks) {
        this.invulnerableTicks = Math.max(0, ticks);
    }

    @Override
    public int invulnerableTicksLeft() {
        return invulnerableTicks;
    }

    @Override
    public void onDamage(Consumer<DamageEvent> listener) {
        damageListeners.add(listener);
    }

    @Override
    public void onDeath(Consumer<DeathEvent> listener) {
        deathListeners.add(listener);
    }

    @Override
    public void onHeal(Consumer<HealEvent> listener) {
        healListeners.add(listener);
    }

    /**
     * Ticks the health component, decrementing invulnerability.
     * Should be called once per game tick.
     */
    public void tick() {
        if (invulnerableTicks > 0) {
            invulnerableTicks--;
        }
    }
}
