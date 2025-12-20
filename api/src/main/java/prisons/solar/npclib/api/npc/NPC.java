package prisons.solar.npclib.api.npc;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.animation.NPCAnimation;
import prisons.solar.npclib.api.appearance.NPCAppearance;
import prisons.solar.npclib.api.combat.Combatant;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.interaction.InteractionHandler;
import prisons.solar.npclib.api.metadata.NPCMetadata;
import prisons.solar.npclib.api.status.EntityStatus;
import prisons.solar.npclib.api.viewer.Viewer;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Represents a packet-based NPC entity.
 * NPCs are client-side only and do not exist as real server entities.
 *
 * @param <A> the appearance type for this NPC
 */
public interface NPC<A extends NPCAppearance> extends Combatant {

    /**
     * Gets the unique identifier for this NPC.
     *
     * @return the NPC's UUID
     */
    @NotNull UUID id();

    // Combatant interface implementations

    @Override
    default UUID getId() {
        return id();
    }

    @Override
    default String getName() {
        return entityType().name();
    }

    /**
     * Gets the entity ID used for packets.
     * This is allocated when the NPC is registered.
     *
     * @return the entity ID, or -1 if not registered
     */
    int entityId();

    /**
     * Gets the entity type of this NPC.
     *
     * @return the entity type
     */
    @NotNull EntityType entityType();

    /**
     * Gets the current lifecycle state.
     *
     * @return the NPC state
     */
    @NotNull NPCState state();

    /**
     * Gets the current position of this NPC.
     *
     * @return the position
     */
    @NotNull Position position();

    /**
     * Gets the metadata container for this NPC.
     *
     * @return the metadata
     */
    @NotNull NPCMetadata metadata();

    /**
     * Gets the appearance configuration for this NPC.
     *
     * @return the appearance
     */
    @NotNull A appearance();

    // Lifecycle methods

    /**
     * Spawns this NPC for all applicable viewers.
     * The NPC must be in REGISTERED or DESPAWNED state.
     */
    void spawn();

    /**
     * Spawns this NPC for a specific viewer.
     *
     * @param viewer the viewer to spawn for
     */
    void spawn(@NotNull Viewer viewer);

    /**
     * Despawns this NPC from all viewers.
     */
    void despawn();

    /**
     * Despawns this NPC from a specific viewer.
     *
     * @param viewer the viewer to despawn for
     */
    void despawn(@NotNull Viewer viewer);

    /**
     * Destroys this NPC, releasing all resources.
     * The NPC cannot be reused after destruction.
     */
    void destroy();

    // Position methods

    /**
     * Teleports this NPC to a new position.
     *
     * @param position the new position
     */
    void teleport(@NotNull Position position);

    /**
     * Makes the NPC look at a position.
     *
     * @param position the position to look at
     */
    void lookAt(@NotNull Position position);

    /**
     * Makes the NPC look at a viewer.
     *
     * @param viewer the viewer to look at
     */
    void lookAt(@NotNull Viewer viewer);

    // Viewer methods

    /**
     * Gets all viewers currently seeing this NPC.
     *
     * @return collection of viewers
     */
    @NotNull Collection<Viewer> viewers();

    /**
     * Checks if a viewer can currently see this NPC.
     *
     * @param viewer the viewer
     * @return true if visible to the viewer
     */
    boolean isVisibleTo(@NotNull Viewer viewer);

    // Interaction methods

    /**
     * Sets the click handler for this NPC.
     *
     * @param handler the click handler
     */
    void onClick(@NotNull InteractionHandler handler);

    /**
     * Sets a consumer to handle click interactions.
     *
     * @param handler the handler consumer
     */
    default void onInteract(@NotNull Consumer<InteractionHandler.Context> handler) {
        onClick(handler::accept);
    }

    // Animation methods

    /**
     * Plays an animation for all viewers.
     *
     * <p>The animation must be supported by this NPC's entity type.
     * Use {@link #supportsAnimation(prisons.solar.npclib.api.animation.NPCAnimation)} to check
     * if an animation is supported before playing it.
     *
     * @param animation the animation to play
     * @throws IllegalArgumentException if the animation is not supported by this entity type
     * @see prisons.solar.npclib.api.animation.EntityAnimation
     * @see prisons.solar.npclib.api.animation.MobAnimation
     * @see prisons.solar.npclib.api.animation.ObjectAnimation
     */
    void playAnimation(@NotNull NPCAnimation animation);

    /**
     * Plays an animation for a specific viewer.
     *
     * <p>The animation must be supported by this NPC's entity type.
     *
     * @param viewer    the viewer to show the animation to
     * @param animation the animation to play
     * @throws IllegalArgumentException if the animation is not supported by this entity type
     * @see prisons.solar.npclib.api.animation.EntityAnimation
     * @see prisons.solar.npclib.api.animation.MobAnimation
     * @see prisons.solar.npclib.api.animation.ObjectAnimation
     */
    void playAnimation(@NotNull Viewer viewer, @NotNull NPCAnimation animation);

    /**
     * Checks if this NPC supports a given animation.
     *
     * <p>Supported animations vary by entity type. All entity types support universal
     * {@link prisons.solar.npclib.api.animation.EntityAnimation} values,
     * while mob NPCs additionally support entity-specific {@link prisons.solar.npclib.api.animation.MobAnimation} values.
     *
     * @param animation the animation to check
     * @return true if this NPC supports the animation, false otherwise
     */
    boolean supportsAnimation(@NotNull NPCAnimation animation);

    /**
     * Gets all animations supported by this NPC's entity type.
     *
     * <p>The returned collection is based on the NPC's entity type and includes
     * both common animations and entity-specific animations.
     *
     * @return unmodifiable collection of supported animations
     */
    @NotNull Collection<NPCAnimation> supportedAnimations();

    // Entity Status methods

    /**
     * Plays an entity status for all viewers.
     *
     * <p>The status must be supported by this NPC's entity type.
     * Use {@link #supportsStatus(prisons.solar.npclib.api.status.EntityStatus)} to check
     * if a status is supported before playing it.
     *
     * @param status the status to play
     * @throws IllegalArgumentException if the status is not supported by this entity type
     * @see prisons.solar.npclib.api.status.PlayerStatus
     * @see prisons.solar.npclib.api.status.MobStatus
     * @see prisons.solar.npclib.api.status.ObjectStatus
     */
    void playStatus(@NotNull EntityStatus status);

    /**
     * Plays an entity status for a specific viewer.
     *
     * <p>The status must be supported by this NPC's entity type.
     *
     * @param viewer the viewer to show the status to
     * @param status the status to play
     * @throws IllegalArgumentException if the status is not supported by this entity type
     * @see prisons.solar.npclib.api.status.PlayerStatus
     * @see prisons.solar.npclib.api.status.MobStatus
     * @see prisons.solar.npclib.api.status.ObjectStatus
     */
    void playStatus(@NotNull Viewer viewer, @NotNull EntityStatus status);

    /**
     * Checks if this NPC supports a given entity status.
     *
     * <p>Supported statuses vary by entity type. For example, player NPCs
     * support {@link prisons.solar.npclib.api.status.PlayerStatus} values,
     * while mob NPCs support {@link prisons.solar.npclib.api.status.MobStatus} values.
     *
     * @param status the status to check
     * @return true if this NPC supports the status, false otherwise
     */
    boolean supportsStatus(@NotNull EntityStatus status);
    /**
     * Gets all entity statuses supported by this NPC's entity type.
     *
     * <p>The returned collection is based on the NPC's entity type and includes
     * both common statuses and entity-specific statuses.
     *
     * @return unmodifiable collection of supported statuses
     */
    @NotNull Collection<EntityStatus> supportedStatuses();
}
