package prisons.solar.npclib.api.animation;

/**
 * Universal animations that work on all entity types.
 *
 * <p>These animations are supported by players, mobs, and other entities
 * through the entity animation packet system. Unlike entity-specific animations,
 * these work universally across all NPC types.
 *
 * <p>For entity-specific animations (like {@code ALLAY_DANCE} or {@code WARDEN_SONIC_BOOM}),
 * see {@link MobAnimation}.
 *
 * @see MobAnimation
 * @see ObjectAnimation
 */
public enum EntityAnimation implements NPCAnimation {

    /**
     * Swing main hand/arm animation.
     * Works on: All entity types
     */
    SWING_MAIN_ARM,

    /**
     * Swing offhand animation.
     * Works on: All entity types
     */
    SWING_OFFHAND,

    /**
     * Hurt/damage animation with red flash effect.
     * Works on: All entity types
     *
     * <p>This animation is handled by a dedicated hurt animation packet
     * for maximum compatibility with client-side NPCs.
     */
    TAKE_DAMAGE,

    /**
     * Critical hit particle effect.
     * Works on: All entity types
     */
    CRITICAL_EFFECT,

    /**
     * Magic critical hit particle effect.
     * Works on: All entity types
     */
    MAGIC_CRITICAL_EFFECT
}
