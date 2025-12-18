package prisons.solar.npclib.api.npc;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.appearance.NPCAppearance;
import prisons.solar.npclib.api.entity.EntityType;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Registry for managing NPCs.
 */
public interface NPCRegistry {

    /**
     * Creates a new NPC with the given type and position.
     *
     * @param type     the entity type
     * @param position the spawn position
     * @param <A>      the appearance type
     * @return the created NPC (not yet registered)
     */
    <A extends NPCAppearance> @NotNull NPC<A> create(@NotNull EntityType type, @NotNull Position position);

    /**
     * Registers an NPC with the engine.
     * This allocates an entity ID and makes the NPC trackable.
     *
     * @param npc the NPC to register
     */
    void register(@NotNull NPC<?> npc);

    /**
     * Unregisters an NPC from the engine.
     * The NPC is despawned from all viewers first.
     *
     * @param npc the NPC to unregister
     */
    void unregister(@NotNull NPC<?> npc);

    /**
     * Gets an NPC by its unique identifier.
     *
     * @param id the NPC's UUID
     * @return the NPC if found
     */
    @NotNull Optional<NPC<?>> byId(@NotNull UUID id);

    /**
     * Gets an NPC by its entity ID.
     *
     * @param entityId the entity ID
     * @return the NPC if found
     */
    @NotNull Optional<NPC<?>> byEntityId(int entityId);

    /**
     * Gets all registered NPCs of a specific type.
     *
     * @param type the entity type
     * @return collection of NPCs
     */
    @NotNull Collection<NPC<?>> byType(@NotNull EntityType type);

    /**
     * Gets all registered NPCs.
     *
     * @return collection of all NPCs
     */
    @NotNull Collection<NPC<?>> all();

    /**
     * Gets all NPCs within a range of a position.
     *
     * @param center the center position
     * @param radius the search radius
     * @return collection of NPCs in range
     */
    @NotNull Collection<NPC<?>> inRange(@NotNull Position center, double radius);

    /**
     * Gets all NPCs in a specific world.
     *
     * @param worldId the world identifier
     * @return collection of NPCs in the world
     */
    @NotNull Collection<NPC<?>> inWorld(@NotNull String worldId);

    /**
     * Gets the number of registered NPCs.
     *
     * @return NPC count
     */
    int count();
}
