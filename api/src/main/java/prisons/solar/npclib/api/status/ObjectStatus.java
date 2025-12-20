package prisons.solar.npclib.api.status;

/**
 * Entity status effects specific to object entity NPCs.
 *
 * <p>These statuses only work on object entities like armor stands.
 * For universal statuses that work across all entity types, see {@link CommonStatus}.
 *
 * @see EntityStatus
 * @see CommonStatus
 */
public enum ObjectStatus implements EntityStatus {

    /**
     * Armor stand wobble animation when hit.
     * Works on: Armor Stand only (entity status 32)
     */
    ARMOR_STAND_HIT
}
