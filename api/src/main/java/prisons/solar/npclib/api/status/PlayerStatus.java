package prisons.solar.npclib.api.status;

/**
 * Entity status effects specific to player NPCs.
 *
 * <p>These statuses only work on player entity types. For universal statuses
 * that work across all entity types, see {@link CommonStatus}.
 *
 * @see EntityStatus
 * @see CommonStatus
 */
public enum PlayerStatus implements EntityStatus {

    /**
     * Plays shield blocking sound.
     * Works on: Players (entity status 29)
     */
    SHIELD_BLOCK,

    /**
     * Plays shield break sound and shows broken item particles.
     * Works on: Players (entity status 30)
     */
    SHIELD_BREAK,

    /**
     * Plays hand swap animation (switching main hand and offhand).
     * Works on: Players (entity status 55)
     */
    SWAP_HANDS
}
