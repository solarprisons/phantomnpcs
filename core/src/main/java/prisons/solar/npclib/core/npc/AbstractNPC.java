package prisons.solar.npclib.core.npc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.ai.GoalSelector;
import prisons.solar.npclib.api.animation.AnimationSupport;
import prisons.solar.npclib.api.animation.NPCAnimation;
import prisons.solar.npclib.api.appearance.NPCAppearance;
import prisons.solar.npclib.api.component.Component;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.interaction.InteractionHandler;
import prisons.solar.npclib.api.math.Vector3d;
import prisons.solar.npclib.api.metadata.NPCMetadata;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.NPCState;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.service.ServiceRegistry;
import prisons.solar.npclib.api.status.EntityStatus;
import prisons.solar.npclib.api.status.EntityStatusSupport;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.core.ai.SimpleGoalSelector;
import prisons.solar.npclib.core.engine.NPCEngine;
import prisons.solar.npclib.core.physics.CollisionManager;
import prisons.solar.npclib.core.physics.PhysicsEngine;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base implementation of {@link NPC}.
 *
 * @param <A> the appearance type
 */
public abstract class AbstractNPC<A extends NPCAppearance> implements NPC<A>  {

    protected final UUID id;
    protected final EntityType entityType;
    protected final SimpleNPCMetadata metadata;
    protected final Set<Viewer> viewers = new CopyOnWriteArraySet<>();
    protected final ServiceRegistry services;
    protected final GoalSelector goalSelector;
    protected final Map<Class<? extends Component>, Component> components = new ConcurrentHashMap<>();

    protected int entityId = -1;
    protected Position position;
    protected final AtomicReference<NPCState> state = new AtomicReference<>(NPCState.UNREGISTERED);
    protected InteractionHandler interactionHandler;
    protected A appearance;
    protected boolean physicsEnabled = false;

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }

    public InteractionHandler getInteractionHandler() {
        return interactionHandler;
    }

    protected AbstractNPC(@NotNull EntityType entityType, @NotNull Position position, @Nullable ServiceRegistry services) {
        this.id = UUID.randomUUID();
        this.entityType = entityType;
        this.position = position;
        this.services = services;
        this.metadata = new SimpleNPCMetadata();
        this.metadata.setDirtyCallback(this::onMetadataChanged);
        this.goalSelector = new SimpleGoalSelector(this);

        if (services != null) {
            services.get(NPCEngine.class).ifPresent(engine ->
                engine.registerGoalSelector(this, goalSelector)
            );
        }
    }

    /**
     * Gets the service registry for this NPC.
     *
     * @return the service registry, or null if not available
     */
    protected ServiceRegistry getServices() {
        return services;
    }

    @Override
    public @NotNull UUID getId() {
        return id;
    }

    @Override
    public int entityId() {
        return entityId;
    }

    @Override
    public @NotNull EntityType entityType() {
        return entityType;
    }

    @Override
    public @NotNull NPCState state() {
        return state.get();
    }

    /**
     * Sets the NPC state.
     *
     * @param newState the new state
     */
    public void setState(@NotNull NPCState newState) {
        this.state.set(newState);
    }

    /**
     * Atomically sets the state if it matches the expected value.
     *
     * @param expected the expected current state
     * @param newState the new state to set
     * @return true if the state was updated, false otherwise
     */
    public boolean compareAndSetState(@NotNull NPCState expected, @NotNull NPCState newState) {
        return this.state.compareAndSet(expected, newState);
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
    public @Nullable GoalSelector getGoalSelector() {
        return goalSelector;
    }

    @Override
    public @NotNull Position getPosition() {
        return position;
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

    @Override
    public boolean supportsAnimation(@NotNull NPCAnimation animation) {
        return AnimationSupport.supportsAnimation(entityType, animation);
    }

    @Override
    public @NotNull Collection<NPCAnimation> supportedAnimations() {
        return AnimationSupport.getSupportedAnimations(entityType);
    }

    @Override
    public boolean supportsStatus(@NotNull EntityStatus status) {
        return EntityStatusSupport.supportsStatus(entityType, status);
    }

    @Override
    public @NotNull Collection<EntityStatus> supportedStatuses() {
        return EntityStatusSupport.getSupportedStatuses(entityType);
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

    @Override
    public void teleport(@NotNull Position position) {
        teleport(position, false);
    }

    @Override
    public void teleport(@NotNull Position position, boolean keepVelocity) {
        this.position = position;

        if (physicsEnabled && services != null) {
            services.get(CollisionManager.class).ifPresent(cm ->
                cm.updatePosition(this, position));

            if (!keepVelocity) {
                services.get(PhysicsEngine.class).ifPresent(pe ->
                    pe.setVelocity(this, Vector3d.ZERO));
            }
        }

        sendTeleportPackets();
    }

    @Override
    public void enablePhysics() {
        this.physicsEnabled = true;

        if (state.get() == NPCState.SPAWNED && services != null) {
            services.get(CollisionManager.class).ifPresent(cm ->
                cm.registerEntity(this));
            services.get(PhysicsEngine.class).ifPresent(pe ->
                pe.registerEntity(this));
        }
    }

    @Override
    public void disablePhysics() {
        this.physicsEnabled = false;

        if (services != null) {
            services.get(CollisionManager.class).ifPresent(cm ->
                cm.unregisterEntity(getId()));
            services.get(PhysicsEngine.class).ifPresent(pe ->
                pe.unregisterEntity(this));
        }
    }

    /**
     * Cleanup method to stop all goals and release resources.
     * Called when NPC is being despawned or destroyed.
     */
    protected void cleanupGoals() {
        if (goalSelector != null) {
            goalSelector.clearGoals();
        }
    }

    @Override
    public boolean hasPhysics() {
        return physicsEnabled;
    }

    @Override
    public void applyImpulse(@NotNull Vector3d impulse) {
        if (!physicsEnabled || services == null) {
            return;
        }

        services.get(PhysicsEngine.class).ifPresent(pe ->
            pe.applyImpulse(this, impulse));
    }

    @Override
    public @NotNull Vector3d getVelocity() {
        if (!physicsEnabled || services == null) {
            return Vector3d.ZERO;
        }

        return services.get(PhysicsEngine.class)
            .flatMap(pe -> pe.getVelocity(this))
            .orElse(Vector3d.ZERO);
    }

    @Override
    public void setVelocity(@NotNull Vector3d velocity) {
        if (!physicsEnabled || services == null) {
            return;
        }

        services.get(PhysicsEngine.class).ifPresent(pe ->
            pe.setVelocity(this, velocity));
    }

    @Override
    public void setTargetVelocity(@NotNull Vector3d targetVelocity) {
        if (!physicsEnabled || services == null) {
            return;
        }

        services.get(PhysicsEngine.class).ifPresent(pe ->
            pe.setTargetVelocity(this, targetVelocity));
    }

    @Override
    public @NotNull Vector3d getTargetVelocity() {
        if (!physicsEnabled || services == null) {
            return Vector3d.ZERO;
        }

        return services.get(PhysicsEngine.class)
            .flatMap(pe -> pe.getTargetVelocity(this))
            .orElse(Vector3d.ZERO);
    }

    @Override
    public boolean isOnGround() {
        if (!physicsEnabled || services == null) {
            return false;
        }

        return services.get(PhysicsEngine.class)
            .flatMap(pe -> pe.isOnGround(this))
            .orElse(false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> Optional<T> getComponent(@NotNull Class<T> type) {
        Component direct = components.get(type);
        if (direct != null) {
            return Optional.of((T) direct);
        }

        for (Map.Entry<Class<? extends Component>, Component> entry : components.entrySet()) {
            if (type.isAssignableFrom(entry.getKey())) {
                return Optional.of((T) entry.getValue());
            }
        }

        return Optional.empty();
    }

    @Override
    public <T extends Component> T addComponent(@NotNull T component) {
        Component existing = components.put(component.getClass(), component);
        if (existing != null) {
            existing.onDetach(this);
        }

        component.onAttach(this);
        return component;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Component> boolean removeComponent(@NotNull Class<T> type) {
        Component removed = components.remove(type);
        if (removed != null) {
            removed.onDetach(this);
            return true;
        }
        return false;
    }

    @Override
    public boolean hasComponent(@NotNull Class<? extends Component> type) {
        if (components.containsKey(type)) {
            return true;
        }

        for (Class<? extends Component> key : components.keySet()) {
            if (type.isAssignableFrom(key)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public @NotNull Collection<Component> getComponents() {
        return Set.copyOf(components.values());
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
