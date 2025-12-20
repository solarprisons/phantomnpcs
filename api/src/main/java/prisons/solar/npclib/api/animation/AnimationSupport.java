package prisons.solar.npclib.api.animation;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.entity.EntityType;

import java.util.*;

/**
 * Utility class for determining which animations are supported by each entity type.
 *
 * <p>This class provides mappings between entity types and their supported animations,
 * handling both common animations that work across entity types and entity-specific
 * animations that only work for particular entities.
 */
public final class AnimationSupport {

    private AnimationSupport() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Gets all animations supported by a specific entity type.
     *
     * @param entityType the entity type
     * @return unmodifiable collection of supported animations
     * @throws IllegalArgumentException if entityType is null
     */
    public static @NotNull Collection<NPCAnimation> getSupportedAnimations(@NotNull EntityType entityType) {
        Objects.requireNonNull(entityType, "entityType cannot be null");

        return switch (entityType.category()) {
            case PLAYER -> getPlayerAnimations();
            case MOB -> getMobAnimations(entityType);
            case DISPLAY -> getDisplayAnimations();
            case OBJECT -> getObjectAnimations(entityType);
        };
    }

    private static @NotNull Collection<NPCAnimation> getPlayerAnimations() {
        // Players support all universal entity animations
        return List.of(EntityAnimation.values());
    }

    private static @NotNull Collection<NPCAnimation> getDisplayAnimations() {
        // Display entities do not support animations
        return List.of();
    }

    /**
     * Checks if a specific entity type supports a given animation.
     *
     * @param entityType the entity type
     * @param animation  the animation to check
     * @return true if the animation is supported, false otherwise
     * @throws IllegalArgumentException if entityType or animation is null
     */
    public static boolean supportsAnimation(@NotNull EntityType entityType, @NotNull NPCAnimation animation) {
        Objects.requireNonNull(entityType, "entityType cannot be null");
        Objects.requireNonNull(animation, "animation cannot be null");

        return getSupportedAnimations(entityType).contains(animation);
    }

    private static @NotNull Collection<NPCAnimation> getMobAnimations(@NotNull EntityType entityType) {
        // All mobs support universal entity animations
        List<NPCAnimation> animations = new ArrayList<>(List.of(EntityAnimation.values()));

        // Entity-specific animations
        switch (entityType) {
            case ALLAY -> animations.add(MobAnimation.ALLAY_DANCE);
            case FOX -> animations.add(MobAnimation.FOX_CHEW);
            case GOAT -> {
                animations.add(MobAnimation.GOAT_PREPARE_RAM);
                animations.add(MobAnimation.GOAT_STOP_RAM);
            }
            case IRON_GOLEM -> animations.add(MobAnimation.IRON_GOLEM_ATTACK);
            case EntityType.WARDEN -> {
                animations.add(MobAnimation.WARDEN_SONIC_BOOM);
                animations.add(MobAnimation.WARDEN_TENDRIL_SHAKING);
            }
            case WOLF -> {
                animations.add(MobAnimation.WOLF_SHAKE_WATER);
                animations.add(MobAnimation.WOLF_STOP_SHAKE_WATER);
            }
            case SHEEP -> animations.add(MobAnimation.SHEEP_EAT_GRASS);
            case HORSE, CAT, DONKEY, MULE -> {
                // Tameable animals
                animations.add(MobAnimation.TAME_SUCCESS);
                animations.add(MobAnimation.TAME_FAIL);
            }
        }

        return Collections.unmodifiableList(animations);
    }

    private static @NotNull Collection<NPCAnimation> getObjectAnimations(@NotNull EntityType entityType) {
        List<NPCAnimation> animations = new ArrayList<>();

        // Add entity-specific animations
        if (entityType == EntityType.ARMOR_STAND) {
            animations.add(ObjectAnimation.ARMOR_STAND_HIT);
        }

        return Collections.unmodifiableList(animations);
    }
}