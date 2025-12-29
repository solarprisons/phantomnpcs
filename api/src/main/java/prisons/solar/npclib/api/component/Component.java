package prisons.solar.npclib.api.component;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;

/**
 * Base interface for NPC components.
 *
 * <p>Components provide modular, attachable capabilities to NPCs following
 * the Entity-Component pattern. This allows features like combat, health,
 * and pathfinding to be optional rather than required for all NPCs.
 *
 * <p>Example usage:
 * <pre>{@code
 * NPC<?> npc = ...;
 * npc.addComponent(new DefaultCombatantComponent(npc, 20.0)); // 20 HP
 *
 * // Later, check if NPC has combat capability
 * npc.getComponent(CombatantComponent.class).ifPresent(combat -> {
 *     combat.healthComponent().damage(5.0);
 * });
 * }</pre>
 *
 * @see prisons.solar.npclib.api.npc.NPC#addComponent(Component)
 * @see prisons.solar.npclib.api.npc.NPC#getComponent(Class)
 */
public interface Component {

    /**
     * Called when this component is attached to an NPC.
     * Use this for initialization that requires the parent NPC.
     *
     * @param npc the NPC this component is being attached to
     */
    void onAttach(@NotNull NPC<?> npc);

    /**
     * Called when this component is detached from an NPC.
     * Use this for cleanup of resources.
     *
     * @param npc the NPC this component is being detached from
     */
    void onDetach(@NotNull NPC<?> npc);

    /**
     * Called each tick while the component is attached.
     * Override this for per-tick logic.
     *
     * <p>Default implementation does nothing.
     */
    default void tick() {
        // No-op by default
    }
}
