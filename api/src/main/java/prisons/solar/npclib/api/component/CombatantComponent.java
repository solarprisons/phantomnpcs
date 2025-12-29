package prisons.solar.npclib.api.component;

import prisons.solar.npclib.api.combat.Combatant;

/**
 * Component that provides combat capability to an NPC.
 *
 * <p>This component makes an NPC a valid combatant that can participate
 * in combat interactions with the {@link prisons.solar.npclib.api.combat.CombatHandler}
 * and {@link prisons.solar.npclib.api.combat.DamageCalculator} systems.
 *
 * <p>Implementation note: This interface extends both {@link Component} and
 * {@link Combatant}, allowing NPCs with this component to be passed to
 * combat-related APIs that expect a {@link Combatant}.
 *
 * <p>Example usage:
 * <pre>{@code
 * NPC<?> npc = ...;
 * CombatantComponent combat = new DefaultCombatantComponent(npc, 20.0);
 * npc.addComponent(combat);
 *
 * // The component can now be used with combat systems
 * damageCalculator.calculateDamage(attacker, combat, 10.0);
 * }</pre>
 *
 * @see Component
 * @see Combatant
 * @see prisons.solar.npclib.api.combat.CombatHandler
 */
public interface CombatantComponent extends Component, Combatant {

    /**
     * Sets whether this combatant is currently blocking.
     *
     * @param blocking true if blocking
     */
    void setBlocking(boolean blocking);
}
