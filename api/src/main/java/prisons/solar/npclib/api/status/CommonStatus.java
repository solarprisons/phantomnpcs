package prisons.solar.npclib.api.status;

/**
 * Universal entity status effects that work across all entity types.
 *
 * <p>These statuses work on all entity types (players, mobs, and objects).
 * They are sent via entity status packets and trigger client-side visual or audio effects.
 *
 * @see EntityStatus
 */
public enum CommonStatus implements EntityStatus {

    /**
     * Plays death animation and sound.
     * Works on: All living entities (entity status 3)
     */
    DEATH,

    /**
     * Plays totem of undying activation effect.
     * Works on: All living entities (entity status 35)
     */
    TOTEM_ACTIVATION,

    /**
     * Shows portal teleportation particles.
     * Works on: All entities (entity status 46)
     */
    PORTAL_PARTICLES,

    /**
     * Shows main hand item break particles.
     * Works on: All living entities (entity status 47)
     */
    BREAK_MAIN_HAND,

    /**
     * Shows offhand item break particles.
     * Works on: All living entities (entity status 48)
     */
    BREAK_OFFHAND,

    /**
     * Shows helmet break particles.
     * Works on: All living entities (entity status 49)
     */
    BREAK_HELMET,

    /**
     * Shows chestplate break particles.
     * Works on: All living entities (entity status 50)
     */
    BREAK_CHESTPLATE,

    /**
     * Shows leggings break particles.
     * Works on: All living entities (entity status 51)
     */
    BREAK_LEGGINGS,

    /**
     * Shows boots break particles.
     * Works on: All living entities (entity status 52)
     */
    BREAK_BOOTS,

    /**
     * Shows honey block slide particles.
     * Works on: All living entities (entity status 54)
     */
    HONEY_SLIDE_PARTICLES
}
