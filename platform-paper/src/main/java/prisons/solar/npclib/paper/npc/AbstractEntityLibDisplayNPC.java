package prisons.solar.npclib.paper.npc;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.world.Location;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.DisplayAppearance;
import prisons.solar.npclib.api.npc.NPCState;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.status.EntityStatus;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.core.event.SimpleEventBus;
import prisons.solar.npclib.core.event.SimpleNPCDespawnEvent;
import prisons.solar.npclib.core.event.SimpleNPCSpawnEvent;
import prisons.solar.npclib.core.npc.AbstractNPC;
import prisons.solar.npclib.core.physics.CollisionManager;
import prisons.solar.npclib.core.physics.PhysicsEngine;
import prisons.solar.npclib.paper.PaperViewer;
import prisons.solar.npclib.paper.status.StatusHandlers;

public abstract class AbstractEntityLibDisplayNPC<A extends DisplayAppearance> extends AbstractNPC<A> {

    protected WrapperEntity wrapperEntity;
    protected SimpleEventBus eventBus;

    public AbstractEntityLibDisplayNPC(@NotNull prisons.solar.npclib.api.entity.EntityType entityType,
                                       @NotNull Position position,
                                       @Nullable prisons.solar.npclib.api.service.ServiceRegistry services) {
        super(entityType, position, services);
    }

    protected abstract EntityType getPacketEventsEntityType();

    protected abstract void applySpecificMeta(AbstractDisplayMeta meta);

    public void setEventBus(@Nullable SimpleEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void spawn() {
        if (state.get() != NPCState.REGISTERED && state.get() != NPCState.DESPAWNED) {
            throw new IllegalStateException("NPC must be registered before spawning");
        }
        setState(NPCState.SPAWNED);

        // Register physics if enabled
        if (physicsEnabled && services != null) {
            services.get(CollisionManager.class).ifPresent(cm ->
                cm.registerEntity(this));
            services.get(PhysicsEngine.class).ifPresent(pe ->
                pe.registerEntity(this));
        }
    }

    @Override
    public void spawn(@NotNull Viewer viewer) {
        if (state.get() == NPCState.UNREGISTERED || state.get() == NPCState.DESTROYED) {
            return;
        }

        if (eventBus != null) {
            SimpleNPCSpawnEvent event = new SimpleNPCSpawnEvent(this, viewer);
            eventBus.post(event);
            if (event.isCancelled()) {
                return;
            }
        }

        if (!viewers.contains(viewer)) {
            addViewer(viewer);
            sendSpawnPackets(viewer);
        }
    }

    @Override
    public void despawn() {
        if (state.get() == NPCState.DESTROYED) {
            return;
        }

        // Clean up goals BEFORE unregistering physics
        cleanupGoals();

        // Unregister physics if enabled
        if (physicsEnabled && services != null) {
            services.get(CollisionManager.class).ifPresent(cm ->
                cm.unregisterEntity(getId()));
            services.get(PhysicsEngine.class).ifPresent(pe ->
                pe.unregisterEntity(this));
        }

        for (Viewer viewer : viewers) {
            if (eventBus != null) {
                eventBus.post(new SimpleNPCDespawnEvent(this, viewer));
            }
            sendDespawnPackets(viewer);
        }
        viewers.clear();
        setState(NPCState.DESPAWNED);
    }

    @Override
    public void despawn(@NotNull Viewer viewer) {
        if (viewers.remove(viewer)) {
            if (eventBus != null) {
                eventBus.post(new SimpleNPCDespawnEvent(this, viewer));
            }
            sendDespawnPackets(viewer);
        }
    }

    @Override
    public void destroy() {
        // Clean up goals BEFORE anything else
        cleanupGoals();

        // Always unregister physics on destroy
        if (services != null) {
            services.get(CollisionManager.class).ifPresent(cm ->
                cm.unregisterEntity(getId()));
            services.get(PhysicsEngine.class).ifPresent(pe ->
                pe.unregisterEntity(this));
        }

        despawn();
        setState(NPCState.DESTROYED);
        if (wrapperEntity != null) {
            wrapperEntity.remove();
            wrapperEntity = null;
        }
    }

    @Override
    public void teleport(@NotNull Position position) {
        this.position = position;
        sendTeleportPackets();
    }

    @Override
    public void lookAt(@NotNull Position target) {
        double dx = target.x() - position.x();
        double dy = target.y() - position.y();
        double dz = target.z() - position.z();

        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distXZ));

        this.position = position.withRotation(yaw, pitch);
        sendLookPackets(yaw, pitch);
    }

    @Override
    public void lookAt(@NotNull Viewer viewer) {
        lookAt(viewer.position());
    }

    @Override
    public void playAnimation(@NotNull prisons.solar.npclib.api.animation.NPCAnimation animation) {
        // Display entities do not support animations
    }

    @Override
    public void playAnimation(@NotNull Viewer viewer, @NotNull prisons.solar.npclib.api.animation.NPCAnimation animation) {
        // Display entities do not support animations
    }

    @Override
    public void playStatus(@NotNull EntityStatus status) {
        if (entityId == -1) {
            return;
        }
        StatusHandlers.playStatus(entityType, entityId, status, viewers);
    }

    @Override
    public void playStatus(@NotNull Viewer viewer, @NotNull EntityStatus status) {
        if (entityId == -1) {
            return;
        }
        StatusHandlers.playStatus(entityType, entityId, status, java.util.List.of(viewer));
    }

    @Override
    protected void onMetadataChanged() {
        syncMetadataToWrapper();
    }

    @SuppressWarnings("unchecked")
    public void syncMetadataToWrapper() {
        if (wrapperEntity == null) {
            return;
        }

        wrapperEntity.consumeEntityMeta(AbstractDisplayMeta.class, meta -> {
            DisplayAppearance displayAppearance = appearance();

            meta.setOnFire(displayAppearance.isOnFire());
            meta.setInvisible(displayAppearance.isInvisible());
            meta.setGlowing(displayAppearance.isGlowing());

            meta.setBillboardConstraints(convertBillboard(displayAppearance.getBillboardMode()));
            meta.setTransformationInterpolationDuration(displayAppearance.getInterpolationDuration());
            meta.setInterpolationDelay(displayAppearance.getInterpolationDelay());
            meta.setViewRange(displayAppearance.getViewRange());
            meta.setShadowRadius(displayAppearance.getShadowRadius());
            meta.setShadowStrength(displayAppearance.getShadowStrength());
            meta.setWidth(displayAppearance.getDisplayWidth());
            meta.setHeight(displayAppearance.getDisplayHeight());

            Integer blockBrightness = displayAppearance.getBlockBrightness();
            Integer skyBrightness = displayAppearance.getSkyBrightness();
            if (blockBrightness != null && skyBrightness != null) {
                int packed = (blockBrightness & 0xF) << 4 | (skyBrightness & 0xF) << 20;
                meta.setBrightnessOverride(packed);
            }

            Integer glowColor = displayAppearance.getGlowColorOverride();
            if (glowColor != null) {
                meta.setGlowColorOverride(glowColor);
            }

            applySpecificMeta(meta);
        });

        wrapperEntity.refresh();
    }

    @Override
    protected void sendSpawnPackets(@NotNull Viewer viewer) {
        if (!(viewer instanceof PaperViewer paperViewer)) {
            return;
        }

        ensureWrapperEntity();

        wrapperEntity.addViewer(paperViewer.getPlayer().getUniqueId());

        Location location = new Location(
                position.x(), position.y(), position.z(), position.yaw(), position.pitch()
        );
        wrapperEntity.spawn(location);
    }

    @Override
    protected void sendDespawnPackets(@NotNull Viewer viewer) {
        if (wrapperEntity != null && viewer instanceof PaperViewer paperViewer) {
            wrapperEntity.removeViewer(paperViewer.getPlayer().getUniqueId());
        }
    }

    @Override
    protected void sendTeleportPackets() {
        if (wrapperEntity != null) {
            Location location = new Location(
                    position.x(), position.y(), position.z(), position.yaw(), position.pitch()
            );
            wrapperEntity.teleport(location);
        }
    }

    @Override
    protected void sendLookPackets(float yaw, float pitch) {
        if (wrapperEntity != null) {
            Location location = new Location(
                    position.x(), position.y(), position.z(), yaw, pitch
            );
            wrapperEntity.teleport(location);
        }
    }

    protected void ensureWrapperEntity() {
        if (wrapperEntity == null) {
            wrapperEntity = new WrapperEntity(entityId, id, getPacketEventsEntityType());
            syncMetadataToWrapper();
        }
    }

    private AbstractDisplayMeta.BillboardConstraints convertBillboard(DisplayAppearance.BillboardMode mode) {
        return switch (mode) {
            case FIXED -> AbstractDisplayMeta.BillboardConstraints.FIXED;
            case VERTICAL -> AbstractDisplayMeta.BillboardConstraints.VERTICAL;
            case HORIZONTAL -> AbstractDisplayMeta.BillboardConstraints.HORIZONTAL;
            case CENTER -> AbstractDisplayMeta.BillboardConstraints.CENTER;
        };
    }

    public WrapperEntity getWrapperEntity() {
        return wrapperEntity;
    }
}
