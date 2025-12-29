package prisons.solar.npclib.paper.animation;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.animation.NPCAnimation;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.viewer.Viewer;

import java.util.Collection;
import java.util.List;

/**
 * Registry for animation handlers.
 *
 * <p>This class manages the different animation handlers and delegates animation
 * playback to the appropriate handler based on the entity type and animation.
 */
public final class AnimationHandlers {

    private static final List<AnimationHandler> HANDLERS = List.of(
            new HurtAnimationHandler(),              // Try dedicated hurt animation packet first
            new EntityStatusAnimationHandler(),
            new CommonAnimationHandler()
    );

    private AnimationHandlers() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Plays an animation for the given entity and viewers.
     *
     * @param entityType the entity type
     * @param entityId   the entity ID
     * @param animation  the animation to play
     * @param viewers    the viewers to show the animation to
     * @throws IllegalArgumentException if no handler supports the animation for this entity type
     */
    public static void playAnimation(@NotNull EntityType entityType,
                                      int entityId,
                                      @NotNull NPCAnimation animation,
                                      @NotNull Collection<Viewer> viewers) {
        for (AnimationHandler handler : HANDLERS) {
            if (handler.supports(entityType, animation)) {
                handler.playAnimation(entityId, animation, viewers);
                return;
            }
        }

        throw new IllegalArgumentException(
                "Animation " + animation + " is not supported for entity type " + entityType
        );
    }

    /**
     * Checks if an animation is supported for the given entity type.
     *
     * @param entityType the entity type
     * @param animation  the animation to check
     * @return true if the animation is supported
     */
    public static boolean supportsAnimation(@NotNull EntityType entityType, @NotNull NPCAnimation animation) {
        return HANDLERS.stream()
                .anyMatch(handler -> handler.supports(entityType, animation));
    }
}
