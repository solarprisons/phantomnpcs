package prisons.solar.npclib.api.animation;

/**
 * Entity-specific animations for mob NPCs.
 *
 * <p>These animations are specific to certain mob types and use entity status packets.
 * Each animation only works on specific entity types as documented below.
 *
 * <p>For universal animations that work on all entity types (like {@code SWING_MAIN_ARM}
 * or {@code TAKE_DAMAGE}), see {@link EntityAnimation}.
 *
 * @see EntityAnimation
 * @see ObjectAnimation
 */
public enum MobAnimation implements NPCAnimation {

    /**
     * Allay jukeboxing/dancing animation.
     * Works on: Allay only (entity status 60)
     */
    ALLAY_DANCE,

    /**
     * Fox chewing animation.
     * Works on: Fox only (entity status 45)
     */
    FOX_CHEW,

    /**
     * Goat ram preparation animation.
     * Works on: Goat only (entity status 58)
     */
    GOAT_PREPARE_RAM,

    /**
     * Goat stop ramming animation.
     * Works on: Goat only (entity status 59)
     */
    GOAT_STOP_RAM,

    /**
     * Iron golem attack animation.
     * Works on: Iron Golem only (entity status 4)
     */
    IRON_GOLEM_ATTACK,

    /**
     * Ravager attack animation.
     * Works on: Ravager only (entity status 4)
     */
    RAVAGER_ATTACK,

    /**
     * Warden sonic boom animation.
     * Works on: Warden only (entity status 61)
     */
    WARDEN_SONIC_BOOM,

    /**
     * Warden tendril shaking animation.
     * Works on: Warden only (entity status 62)
     */
    WARDEN_TENDRIL_SHAKING,

    /**
     * Wolf water shake animation.
     * Works on: Wolf only (entity status 8)
     */
    WOLF_SHAKE_WATER,

    /**
     * Wolf stop water shake animation.
     * Works on: Wolf only (entity status 56)
     */
    WOLF_STOP_SHAKE_WATER,

    /**
     * Sheep eat grass animation.
     * Works on: Sheep only (entity status 10)
     */
    SHEEP_EAT_GRASS,

    /**
     * Taming success animation with hearts.
     * Works on: Tameable mobs (entity status 7)
     */
    TAME_SUCCESS,

    /**
     * Taming failure animation with smoke.
     * Works on: Tameable mobs (entity status 6)
     */
    TAME_FAIL
}
