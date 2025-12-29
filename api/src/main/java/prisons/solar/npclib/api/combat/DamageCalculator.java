package prisons.solar.npclib.api.combat;

import prisons.solar.npclib.api.health.HealthComponent;

/**
 * Calculates final damage values for combat interactions.
 *
 * <p>The damage calculator applies modifiers such as armor reduction,
 * blocking, and status effects to determine the final damage dealt.
 *
 * <p>Damage calculation typically flows:
 * <ol>
 *   <li>Base damage from attack</li>
 *   <li>Apply armor reduction via {@link #applyArmorReduction}</li>
 *   <li>Apply effects (enchantments, potions) via {@link #applyEffects}</li>
 *   <li>Final result from {@link #calculateDamage}</li>
 * </ol>
 *
 * @see CombatHandler
 * @see Combatant
 */
public interface DamageCalculator {

    /**
     * Calculates the final damage to be dealt to a target.
     *
     * @param source     the damage source containing type and attacker info
     * @param baseDamage the base damage before modifiers
     * @param target     the target combatant
     * @return the final calculated damage
     */
    float calculateDamage(HealthComponent.DamageSource source, float baseDamage, Combatant target);

    /**
     * Applies armor reduction to the damage.
     *
     * @param damage the damage before armor reduction
     * @param target the target combatant
     * @return the damage after armor reduction
     */
    float applyArmorReduction(float damage, Combatant target);

    /**
     * Applies status effects and enchantments to the damage.
     *
     * @param damage the damage before effects
     * @param target the target combatant
     * @return the damage after applying effects
     */
    float applyEffects(float damage, Combatant target);
}
