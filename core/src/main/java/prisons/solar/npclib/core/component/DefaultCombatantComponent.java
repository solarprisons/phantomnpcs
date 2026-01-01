package prisons.solar.npclib.core.component;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.component.CombatantComponent;
import prisons.solar.npclib.api.health.HealthComponent;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.core.health.DefaultHealthComponent;

import java.util.UUID;

/**
 * Default implementation of {@link CombatantComponent}.
 * Provides combat capability to NPCs including health management.
 *
 * <p>Example usage:
 * <pre>{@code
 * NPC<?> npc = ...;
 * CombatantComponent combat = new DefaultCombatantComponent(20.0f); // 20 HP
 * npc.addComponent(combat);
 *
 * // Later, damage the NPC
 * npc.getComponent(CombatantComponent.class).ifPresent(c -> {
 *     c.healthComponent().damage(
 *         new DamageSource(DamageType.ENTITY_ATTACK, attacker, null, attacker.getPosition(), isCritical),
 *         5.0f
 *     );
 * });
 * }</pre>
 */
public class DefaultCombatantComponent implements CombatantComponent {

    private NPC<?> npc;
    private final DefaultHealthComponent health;
    private boolean blocking = false;

    /**
     * Creates a combatant component with the specified max health.
     *
     * @param maxHealth the maximum health
     */
    public DefaultCombatantComponent(float maxHealth) {
        this.health = new DefaultHealthComponent(maxHealth);
    }

    /**
     * Creates a combatant component with the specified health component.
     *
     * @param healthComponent the health component to use
     */
    public DefaultCombatantComponent(@NotNull DefaultHealthComponent healthComponent) {
        this.health = healthComponent;
    }

    @Override
    public void onAttach(@NotNull NPC<?> npc) {
        this.npc = npc;
    }

    @Override
    public void onDetach(@NotNull NPC<?> npc) {
        this.npc = null;
    }

    @Override
    public void tick() {
        health.tick();
    }

    @Override
    public UUID getId() {
        return npc != null ? npc.getId() : null;
    }

    @Override
    public String getName() {
        return npc != null ? npc.getName() : "Unknown";
    }

    @Override
    public Position getPosition() {
        return npc != null ? npc.getPosition() : null;
    }

    @Override
    public HealthComponent healthComponent() {
        return health;
    }

    @Override
    public boolean isBlocking() {
        return blocking;
    }

    @Override
    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    /**
     * Gets the parent NPC this component is attached to.
     *
     * @return the parent NPC, or null if not attached
     */
    public NPC<?> getNpc() {
        return npc;
    }
}
