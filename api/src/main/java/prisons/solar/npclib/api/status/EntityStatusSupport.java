package prisons.solar.npclib.api.status;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.entity.EntityType;

import java.util.*;

/**
 * Utility class for determining which entity statuses are supported by each entity type.
 *
 * <p>This class provides mappings between entity types and their supported statuses,
 * handling both common statuses that work across entity types and entity-specific
 * statuses that only work for particular entities.
 */
public final class EntityStatusSupport {

    private EntityStatusSupport() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Gets all entity statuses supported by a specific entity type.
     *
     * @param entityType the entity type
     * @return unmodifiable collection of supported statuses
     * @throws IllegalArgumentException if entityType is null
     */
    public static @NotNull Collection<EntityStatus> getSupportedStatuses(@NotNull EntityType entityType) {
        Objects.requireNonNull(entityType, "entityType cannot be null");

        return switch (entityType.category()) {
            case PLAYER -> getPlayerStatuses();
            case MOB -> getMobStatuses(entityType);
            case DISPLAY -> getDisplayStatuses();
            case OBJECT -> getObjectStatuses(entityType);
        };
    }

    /**
     * Checks if a specific entity type supports a given status.
     *
     * @param entityType the entity type
     * @param status     the status to check
     * @return true if the status is supported, false otherwise
     * @throws IllegalArgumentException if entityType or status is null
     */
    public static boolean supportsStatus(@NotNull EntityType entityType, @NotNull EntityStatus status) {
        Objects.requireNonNull(entityType, "entityType cannot be null");
        Objects.requireNonNull(status, "status cannot be null");

        return getSupportedStatuses(entityType).contains(status);
    }

    private static @NotNull Collection<EntityStatus> getPlayerStatuses() {
        List<EntityStatus> statuses = new ArrayList<>();
        // Add all common statuses
        Collections.addAll(statuses, CommonStatus.values());
        // Add all player-specific statuses
        Collections.addAll(statuses, PlayerStatus.values());
        return Collections.unmodifiableList(statuses);
    }

    private static @NotNull Collection<EntityStatus> getDisplayStatuses() {
        // Display entities only support PORTAL_PARTICLES
        return List.of(CommonStatus.PORTAL_PARTICLES);
    }

    private static @NotNull Collection<EntityStatus> getMobStatuses(@NotNull EntityType entityType) {
        List<EntityStatus> statuses = new ArrayList<>();

        // All mobs support common statuses
        Collections.addAll(statuses, CommonStatus.values());

        // Add entity-specific statuses
        switch (entityType) {
            case ALLAY -> statuses.add(MobStatus.ALLAY_DANCE);
            case FOX -> statuses.add(MobStatus.FOX_CHEW);
            case GOAT -> {
                statuses.add(MobStatus.GOAT_PREPARE_RAM);
                statuses.add(MobStatus.GOAT_STOP_RAM);
            }
            case IRON_GOLEM -> statuses.add(MobStatus.ATTACK);
            case EntityType.WARDEN -> {
                statuses.add(MobStatus.ATTACK);
                statuses.add(MobStatus.WARDEN_SONIC_BOOM);
                statuses.add(MobStatus.WARDEN_TENDRIL_SHAKING);
            }
            case WOLF -> {
                statuses.add(MobStatus.WOLF_SHAKE_START);
                statuses.add(MobStatus.WOLF_SHAKE_STOP);
            }
            case SHEEP -> statuses.add(MobStatus.SHEEP_EAT_GRASS);
            case HORSE, CAT, EntityType.DONKEY, EntityType.MULE -> {
                // Tameable animals
                statuses.add(MobStatus.TAME_SUCCESS);
                statuses.add(MobStatus.TAME_FAIL);
            }
            // Breedable animals support LOVE_HEARTS
            case COW, PIG, CHICKEN, LLAMA, PANDA, BEE, FROG, AXOLOTL, CAMEL, SNIFFER, ARMADILLO -> {
                statuses.add(MobStatus.LOVE_HEARTS);
            }
        }

        // Add LOVE_HEARTS to entities that already have other statuses but are also breedable
        if (entityType == EntityType.SHEEP || entityType == EntityType.FOX ||
            entityType == EntityType.GOAT || entityType == EntityType.WOLF ||
            entityType == EntityType.CAT || entityType == EntityType.HORSE ||
            entityType == EntityType.DONKEY || entityType == EntityType.MULE) {
            statuses.add(MobStatus.LOVE_HEARTS);
        }

        return Collections.unmodifiableList(statuses);
    }

    private static @NotNull Collection<EntityStatus> getObjectStatuses(@NotNull EntityType entityType) {
        List<EntityStatus> statuses = new ArrayList<>();

        // Add entity-specific statuses
        if (entityType == EntityType.ARMOR_STAND) {
            statuses.add(ObjectStatus.ARMOR_STAND_HIT);
        }

        // All objects support PORTAL_PARTICLES
        statuses.add(CommonStatus.PORTAL_PARTICLES);

        return Collections.unmodifiableList(statuses);
    }
}
