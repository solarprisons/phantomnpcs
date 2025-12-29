package prisons.solar.npclib.paper.animation;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.animation.NPCAnimation;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.viewer.Viewer;

import java.util.Collection;

/**
 * Strategy interface for handling different types of NPC animations.
 *
 * <p>Implementations delegate to different underlying systems:
 * <ul>
 *   <li>{@link EntityLibAnimationHandler} - Uses EntityLib wrapper methods for common animations</li>
 *   <li>{@link PacketEventsAnimationHandler} - Uses PacketEvents animation packets for player animations</li>
 *   <li>{@link EntityStatusAnimationHandler} - Uses entity status packets for entity-specific animations</li>
 * </ul>
 */
interface AnimationHandler {

    /**
     * Checks if this handler supports the given animation for the entity type.
     *
     * @param type      the entity type
     * @param animation the animation to check
     * @return true if this handler can play the animation for this entity type
     */
    boolean supports(@NotNull EntityType type, @NotNull NPCAnimation animation);

    /**
     * Plays an animation for the given viewers.
     *
     * @param entityId  the entity ID
     * @param animation the animation to play
     * @param viewers   the viewers to show the animation to
     * @throws IllegalArgumentException if this handler doesn't support the animation
     */
    void playAnimation(int entityId, @NotNull NPCAnimation animation, @NotNull Collection<Viewer> viewers);
}
