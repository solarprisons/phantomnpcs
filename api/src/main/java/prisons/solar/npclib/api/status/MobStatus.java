package prisons.solar.npclib.api.status;

/**
 * Entity status effects specific to living entity (mob) NPCs.
 *
 * <p>These statuses only work on specific mob types. For universal statuses
 * that work across all entity types, see {@link CommonStatus}.
 *
 * <p>Entity-specific statuses are sent via entity status packets and will
 * only display if the NPC's entity type supports that status.
 *
 * @see EntityStatus
 * @see CommonStatus
 */
public enum MobStatus implements EntityStatus {

    /**
     * Attack animation and sound.
     * Works on: Iron Golem, Ravager, Warden (entity status 4)
     */
    ATTACK,

    /**
     * Taming failed with smoke particles.
     * Works on: Tameable animals (horse, wolf, cat, parrot) (entity status 6)
     */
    TAME_FAIL,

    /**
     * Taming succeeded with heart particles.
     * Works on: Tameable animals (horse, wolf, cat, parrot) (entity status 7)
     */
    TAME_SUCCESS,

    /**
     * Wolf starts shaking water off.
     * Works on: Wolf only (entity status 8)
     */
    WOLF_SHAKE_START,

    /**
     * Sheep eating grass animation (lasts 40 ticks).
     * Works on: Sheep only (entity status 10)
     */
    SHEEP_EAT_GRASS,

    /**
     * Shows love/breeding heart particles.
     * Works on: Breedable animals (entity status 18)
     */
    LOVE_HEARTS,

    /**
     * Fox chewing/eating animation.
     * Works on: Fox only (entity status 45)
     */
    FOX_CHEW,

    /**
     * Wolf stops shaking water.
     * Works on: Wolf only (entity status 56)
     */
    WOLF_SHAKE_STOP,

    /**
     * Goat lowers head to prepare ram attack.
     * Works on: Goat only (entity status 58)
     */
    GOAT_PREPARE_RAM,

    /**
     * Goat stops lowering head/cancels ram.
     * Works on: Goat only (entity status 59)
     */
    GOAT_STOP_RAM,

    /**
     * Allay jukeboxing/dancing animation.
     * Works on: Allay only (entity status 60)
     */
    ALLAY_DANCE,

    /**
     * Warden sonic boom attack animation.
     * Works on: Warden only (entity status 61)
     */
    WARDEN_SONIC_BOOM,

    /**
     * Warden tendril shaking animation.
     * Works on: Warden only (entity status 62)
     */
    WARDEN_TENDRIL_SHAKING
}
