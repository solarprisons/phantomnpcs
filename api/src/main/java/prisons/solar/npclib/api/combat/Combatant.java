package prisons.solar.npclib.api.combat;

import prisons.solar.npclib.api.health.HealthComponent;
import prisons.solar.npclib.api.npc.Position;

import java.util.UUID;

/**
 * Represents an entity that can participate in combat.
 *
 * <p>Combatants have health, can take damage, and can be targeted by
 * combat systems. This interface is typically implemented by
 * {@link prisons.solar.npclib.api.component.CombatantComponent} which
 * can be attached to NPCs.
 *
 * @see prisons.solar.npclib.api.component.CombatantComponent
 * @see CombatHandler
 * @see DamageCalculator
 */
public interface Combatant {

    /**
     * Gets the unique identifier for this combatant.
     *
     * @return the combatant's UUID
     */
    UUID getId();

    /**
     * Gets the display name of this combatant.
     *
     * @return the combatant's name
     */
    String getName();

    /**
     * Gets the current position of this combatant.
     *
     * @return the current position
     */
    Position getPosition();

    /**
     * Gets the health component for this combatant.
     *
     * @return the health component
     */
    HealthComponent healthComponent();

    /**
     * Checks if this combatant is currently blocking.
     * Blocking typically reduces incoming damage.
     *
     * @return true if blocking
     */
    boolean isBlocking();
}
