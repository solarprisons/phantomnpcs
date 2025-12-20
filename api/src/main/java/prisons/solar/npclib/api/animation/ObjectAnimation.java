package prisons.solar.npclib.api.animation;

/**
 * Animations available for object entity NPCs (armor stands, etc.).
 *
 * <p>Object entities have limited animation support compared to living entities.
 */
public enum ObjectAnimation implements NPCAnimation {

    /**
     * Armor stand hit animation and sound (with cooldown).
     * Works on: Armor Stand only (entity status 32)
     */
    ARMOR_STAND_HIT
}
