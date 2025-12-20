package prisons.solar.npclib.api.animation;

/**
 * Base interface for all NPC animations.
 *
 * <p>Animations are category-based and vary by entity type:
 * <ul>
 *     <li>{@link EntityAnimation} - Universal animations for all entity types</li>
 *     <li>{@link MobAnimation} - Entity-specific animations for mobs</li>
 *     <li>{@link ObjectAnimation} - Object entities (armor stands)</li>
 * </ul>
 *
 * @see EntityAnimation
 * @see MobAnimation
 * @see ObjectAnimation
 */
public interface NPCAnimation {

    /**
     * Returns the name of this animation.
     *
     * @return the animation name
     */
    String name();
}
