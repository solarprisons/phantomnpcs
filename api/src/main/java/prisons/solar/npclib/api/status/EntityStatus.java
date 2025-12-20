package prisons.solar.npclib.api.status;

/**
 * Marker interface for all entity status types.
 *
 * <p>Entity statuses are sent via entity status packets and trigger client-side
 * visual or audio effects on entities. Unlike animations which use animation packets,
 * statuses use byte status codes to trigger specific effects.
 *
 * <p>This interface is implemented by:
 * <ul>
 *   <li>{@link PlayerStatus} - Player-specific status effects</li>
 *   <li>{@link MobStatus} - Living entity status effects (common + entity-specific)</li>
 *   <li>{@link ObjectStatus} - Object entity status effects</li>
 * </ul>
 *
 * @see PlayerStatus
 * @see MobStatus
 * @see ObjectStatus
 */
public interface EntityStatus {

    /**
     * Gets the name of this entity status.
     *
     * @return the status name
     */
    String name();
}
