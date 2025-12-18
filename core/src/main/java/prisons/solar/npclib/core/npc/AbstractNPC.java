package prisons.solar.npclib.core.npc;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.appearance.NPCAppearance;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.interaction.InteractionHandler;
import prisons.solar.npclib.api.metadata.NPCMetadata;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.NPCState;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.viewer.Viewer;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Abstract base implementation of {@link NPC}.
 *
 * @param <A> the appearance type
 */
public abstract class AbstractNPC<A extends NPCAppearance> implements NPC<A> {

    protected final UUID id;
    protected final EntityType entityType;
    protected final SimpleNPCMetadata metadata;
    protected final Set<Viewer> viewers = new CopyOnWriteArraySet<>();

    protected int entityId = -1;
    protected Position position;
    protected NPCState state = NPCState.UNREGISTERED;
    protected InteractionHandler interactionHandler;
    protected A appearance;

    protected AbstractNPC(@NotNull EntityType entityType, @NotNull Position position) {
        this.id = UUID.randomUUID();
        this.entityType = entityType;
        this.position = position;
        this.metadata = new SimpleNPCMetadata();
        this.metadata.setDirtyCallback(this::onMetadataChanged);
    }

    @Override
    public @NotNull UUID id() {
        return id;
    }

    @Override
    public int entityId() {
        return entityId;
    }

    /**
     * Sets the entity ID. Called during registration.
     *
     * @param entityId the entity ID
     */
    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public @NotNull EntityType entityType() {
        return entityType;
    }

    @Override
    public @NotNull NPCState state() {
        return state;
    }

    /**
     * Sets the NPC state.
     *
     * @param state the new state
     */
    public void setState(@NotNull NPCState state) {
        this.state = state;
    }

    @Override
    public @NotNull Position position() {
        return position;
    }

    @Override
    public @NotNull NPCMetadata metadata() {
        return metadata;
    }

    @Override
    public @NotNull A appearance() {
        return appearance;
    }

    @Override
    public @NotNull Collection<Viewer> viewers() {
        return Set.copyOf(viewers);
    }

    @Override
    public boolean isVisibleTo(@NotNull Viewer viewer) {
        return viewers.contains(viewer);
    }

    @Override
    public void onClick(@NotNull InteractionHandler handler) {
        this.interactionHandler = handler;
    }

    /**
     * Gets the interaction handler.
     *
     * @return the handler, or null if none set
     */
    public InteractionHandler getInteractionHandler() {
        return interactionHandler;
    }

    /**
     * Adds a viewer to this NPC's viewer list.
     *
     * @param viewer the viewer
     */
    public void addViewer(@NotNull Viewer viewer) {
        viewers.add(viewer);
    }

    /**
     * Removes a viewer from this NPC's viewer list.
     *
     * @param viewer the viewer
     */
    public void removeViewer(@NotNull Viewer viewer) {
        viewers.remove(viewer);
    }

    /**
     * Called when metadata changes.
     */
    protected abstract void onMetadataChanged();

    /**
     * Called to send spawn packets to a viewer.
     *
     * @param viewer the viewer
     */
    protected abstract void sendSpawnPackets(@NotNull Viewer viewer);

    /**
     * Called to send despawn packets to a viewer.
     *
     * @param viewer the viewer
     */
    protected abstract void sendDespawnPackets(@NotNull Viewer viewer);

    /**
     * Called to send teleport packets to viewers.
     */
    protected abstract void sendTeleportPackets();

    /**
     * Called to send look packets to viewers.
     *
     * @param yaw   the yaw
     * @param pitch the pitch
     */
    protected abstract void sendLookPackets(float yaw, float pitch);
}
