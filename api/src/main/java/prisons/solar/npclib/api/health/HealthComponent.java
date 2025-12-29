package prisons.solar.npclib.api.health;

import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.combat.Combatant;
import prisons.solar.npclib.api.entity.types.ProjectileEntity;
import prisons.solar.npclib.api.event.npc.health.DamageEvent;
import prisons.solar.npclib.api.event.npc.health.DeathEvent;
import prisons.solar.npclib.api.event.npc.health.HealEvent;
import prisons.solar.npclib.api.npc.Position;

import java.util.function.Consumer;

/**
 * Manages health state and damage/healing operations for a combatant.
 *
 * <p>The health component tracks current and maximum health, handles
 * damage and healing operations, and provides event callbacks for
 * health-related events.
 *
 * <p>Example usage:
 * <pre>{@code
 * HealthComponent health = combatant.healthComponent();
 *
 * // Register event listeners
 * health.onDamage(event -> System.out.println("Took " + event.damage() + " damage"));
 * health.onDeath(event -> System.out.println("Died!"));
 *
 * // Deal damage
 * DamageSource source = new DamageSource(DamageType.ENTITY_ATTACK, attacker, null, pos);
 * DamageResult result = health.damage(source, 5.0f);
 * }</pre>
 *
 * @see prisons.solar.npclib.api.combat.Combatant
 */
public interface HealthComponent {

    /**
     * Gets the current health.
     *
     * @return current health value
     */
    float health();

    /**
     * Gets the maximum health.
     *
     * @return maximum health value
     */
    float maxHealth();

    /**
     * Checks if this entity is dead (health is zero or below).
     *
     * @return true if dead
     */
    boolean isDead();

    /**
     * Checks if this entity is currently invulnerable to damage.
     *
     * @return true if invulnerable
     */
    boolean isInvulnerable();

    /**
     * Sets the current health, clamped between 0 and max health.
     *
     * @param health the new health value
     */
    void setHealth(float health);

    /**
     * Sets the maximum health. Current health is clamped if needed.
     *
     * @param maxHealth the new maximum health (minimum 1)
     */
    void setMaxHealth(float maxHealth);

    /**
     * Heals this entity by the specified amount.
     * Does nothing if dead or amount is zero or negative.
     *
     * @param amount the amount to heal
     */
    void heal(float amount);

    /**
     * Deals damage to this entity from the specified source.
     *
     * @param source the damage source
     * @param amount the damage amount
     * @return the result of the damage operation
     */
    DamageResult damage(DamageSource source, float amount);

    /**
     * Instantly kills this entity.
     */
    void kill();

    /**
     * Revives this entity to full health.
     */
    void revive();

    /**
     * Sets permanent invulnerability state.
     *
     * @param invulnerable true to make invulnerable
     */
    void setInvulnerable(boolean invulnerable);

    /**
     * Sets temporary invulnerability for a number of ticks.
     *
     * @param ticks the number of ticks to be invulnerable
     */
    void setInvulnerableTicks(int ticks);

    /**
     * Gets the remaining invulnerability ticks.
     *
     * @return ticks remaining, or 0 if not temporarily invulnerable
     */
    int invulnerableTicksLeft();

    /**
     * Registers a listener for damage events.
     *
     * @param listener the damage event listener
     */
    void onDamage(Consumer<DamageEvent> listener);

    /**
     * Registers a listener for death events.
     *
     * @param listener the death event listener
     */
    void onDeath(Consumer<DeathEvent> listener);

    /**
     * Registers a listener for heal events.
     *
     * @param listener the heal event listener
     */
    void onHeal(Consumer<HealEvent> listener);

    /**
     * Represents the source of damage.
     *
     * @param type       the type of damage
     * @param attacker   the attacking combatant, or null if environmental
     * @param projectile the projectile that caused damage, or null
     * @param location   the location where damage originated
     */
    record DamageSource(
            DamageType type,
            @Nullable Combatant attacker,
            @Nullable ProjectileEntity projectile,
            Position location
    ) {}

    /**
     * Represents the result of a damage operation.
     *
     * @param finalDamage  the actual damage dealt after modifiers
     * @param healthBefore health before damage
     * @param healthAfter  health after damage
     * @param isKilled     true if this damage killed the entity
     * @param isBlocked    true if damage was blocked
     */
    record DamageResult(
            double finalDamage,
            double healthBefore,
            double healthAfter,
            boolean isKilled,
            boolean isBlocked
    ) {}

    /**
     * Types of damage that can be dealt.
     */
    enum DamageType {
        /** Melee attack from an entity */
        ENTITY_ATTACK,
        /** Damage from a projectile (arrow, fireball, etc.) */
        PROJECTILE,
        /** Fall damage */
        FALL,
        /** Fire damage (standing in fire) */
        FIRE,
        /** Lava damage */
        LAVA,
        /** Drowning damage */
        DROWNING,
        /** Suffocation damage (inside blocks) */
        SUFFOCATION,
        /** Starvation damage */
        STARVATION,
        /** Poison damage */
        POISON,
        /** Magic damage (potions, wither effect) */
        MAGIC,
        /** Explosion damage */
        EXPLOSION,
        /** Custom damage type for plugin use */
        CUSTOM
    }
}
