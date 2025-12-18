package prisons.solar.npclib.api.npc;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.appearance.NPCAppearance;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.interaction.InteractionHandler;
import prisons.solar.npclib.api.metadata.NPCMetadata;
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
public interface NPC<A extends NPCAppearance> {

    /**
     * Gets the unique identifier for this NPC.
     *
     * @return the NPC's UUID
     */
    @NotNull UUID id();

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
        onClick((InteractionHandler) handler::accept);
    }

    // Animation methods

    /**
     * Plays an animation for all viewers.
     *
     * @param animation the animation to play
     */
    void playAnimation(@NotNull Animation animation);

    /**
     * Plays an animation for a specific viewer.
     *
     * @param viewer    the viewer
     * @param animation the animation to play
     */
    void playAnimation(@NotNull Viewer viewer, @NotNull Animation animation);

    /**
     * Common entity animations.
     */
    enum Animation {
        SWING_MAIN_ARM,
        TAKE_DAMAGE,
        LEAVE_BED,
        SWING_OFFHAND,
        CRITICAL_EFFECT,
        MAGIC_CRITICAL_EFFECT
    }
}
