package prisons.solar.npclib.paper.npc;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.types.LivingEntityMeta;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.LivingAppearance;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.npc.NPCState;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.core.event.SimpleEventBus;
import prisons.solar.npclib.core.event.SimpleNPCDespawnEvent;
import prisons.solar.npclib.core.event.SimpleNPCSpawnEvent;
import prisons.solar.npclib.core.npc.AbstractNPC;
import prisons.solar.npclib.paper.PaperViewer;

/**
 * Living entity NPC implementation using EntityLib.
 * Supports all mob types (zombies, villagers, animals, etc).
 */
public class EntityLibLivingNPC extends AbstractNPC<LivingAppearance> {

    private WrapperLivingEntity wrapperEntity;
    private final EntityLibLivingAppearance livingAppearance;
    private SimpleEventBus eventBus;

    public EntityLibLivingNPC(@NotNull EntityType entityType, @NotNull Position position) {
        super(entityType, position);
        this.livingAppearance = new EntityLibLivingAppearance(this);
        this.appearance = livingAppearance;
    }

    /**
     * Sets the event bus for firing events.
     *
     * @param eventBus the event bus
     */
    public void setEventBus(@Nullable SimpleEventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void spawn() {
        if (state != NPCState.REGISTERED && state != NPCState.DESPAWNED) {
            throw new IllegalStateException("NPC must be registered before spawning");
        }
        state = NPCState.SPAWNED;
    }

    @Override
    public void spawn(@NotNull Viewer viewer) {
        if (state == NPCState.UNREGISTERED || state == NPCState.DESTROYED) {
            return;
        }

        // Fire spawn event
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
        if (state == NPCState.DESTROYED) {
            return;
        }
        for (Viewer viewer : viewers) {
            if (eventBus != null) {
                eventBus.post(new SimpleNPCDespawnEvent(this, viewer));
            }
            sendDespawnPackets(viewer);
        }
        viewers.clear();
        state = NPCState.DESPAWNED;
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
        despawn();
        state = NPCState.DESTROYED;
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
    public void playAnimation(@NotNull Animation animation) {
        for (Viewer viewer : viewers) {
            playAnimation(viewer, animation);
        }
    }

    @Override
    public void playAnimation(@NotNull Viewer viewer, @NotNull Animation animation) {
        if (wrapperEntity != null && viewer instanceof PaperViewer) {
            switch (animation) {
                case SWING_MAIN_ARM -> wrapperEntity.swingMainHand();
                case SWING_OFFHAND -> wrapperEntity.swingOffHand();
                case TAKE_DAMAGE -> wrapperEntity.playHurtAnimation();
                case CRITICAL_EFFECT -> wrapperEntity.playCriticalHitAnimation();
                case MAGIC_CRITICAL_EFFECT -> wrapperEntity.playMagicCriticalHitAnimation();
                case LEAVE_BED -> wrapperEntity.playWakeupAnimation();
            }
        }
    }

    @Override
    protected void onMetadataChanged() {
        syncMetadataToWrapper();
    }

    /**
     * Syncs metadata to the EntityLib wrapper.
     */
    public void syncMetadataToWrapper() {
        if (wrapperEntity == null) {
            return;
        }

        wrapperEntity.consumeMeta(meta -> {
            meta.setOnFire(livingAppearance.isOnFire());
            meta.setInvisible(livingAppearance.isInvisible());
            meta.setGlowing(livingAppearance.isGlowing());

            if (!livingAppearance.getCustomName().isEmpty()) {
                meta.setCustomName(Component.text(livingAppearance.getCustomName()));
                meta.setCustomNameVisible(livingAppearance.isCustomNameVisible());
            }

            meta.setPose(convertPose(livingAppearance.getPose()));
        });

        wrapperEntity.refresh();
    }

    /**
     * Syncs equipment to all viewers.
     */
    public void syncEquipment() {
        if (wrapperEntity == null) {
            return;
        }

        var equipment = wrapperEntity.getEquipment();

        for (LivingAppearance.EquipmentSlot slot : LivingAppearance.EquipmentSlot.values()) {
            Object item = livingAppearance.getEquipment(slot);
            if (item instanceof org.bukkit.inventory.ItemStack bukkitItem) {
                com.github.retrooper.packetevents.protocol.item.ItemStack peItem =
                        io.github.retrooper.packetevents.util.SpigotConversionUtil.fromBukkitItemStack(bukkitItem);

                com.github.retrooper.packetevents.protocol.player.EquipmentSlot peSlot = switch (slot) {
                    case MAIN_HAND -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.MAIN_HAND;
                    case OFF_HAND -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.OFF_HAND;
                    case HEAD -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.HELMET;
                    case CHEST -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.CHEST_PLATE;
                    case LEGS -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.LEGGINGS;
                    case FEET -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.BOOTS;
                };

                equipment.setItem(peSlot, peItem);
            }
        }
    }

    @Override
    protected void sendSpawnPackets(@NotNull Viewer viewer) {
        if (!(viewer instanceof PaperViewer paperViewer)) {
            return;
        }

        ensureWrapperEntity();

        wrapperEntity.addViewer(paperViewer.getPlayer().getUniqueId());

        var location = new com.github.retrooper.packetevents.protocol.world.Location(
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
            var location = new com.github.retrooper.packetevents.protocol.world.Location(
                    position.x(), position.y(), position.z(), position.yaw(), position.pitch()
            );
            wrapperEntity.teleport(location);
        }
    }

    @Override
    protected void sendLookPackets(float yaw, float pitch) {
        if (wrapperEntity != null) {
            // Living entities use teleport for rotation
            var location = new com.github.retrooper.packetevents.protocol.world.Location(
                    position.x(), position.y(), position.z(), yaw, pitch
            );
            wrapperEntity.teleport(location);
        }
    }

    private void ensureWrapperEntity() {
        if (wrapperEntity == null) {
            com.github.retrooper.packetevents.protocol.entity.type.EntityType peType = convertEntityType(entityType);
            wrapperEntity = new WrapperLivingEntity(entityId, id, peType);
            syncMetadataToWrapper();
        }
    }

    private com.github.retrooper.packetevents.protocol.entity.type.EntityType convertEntityType(EntityType type) {
        // Map our EntityType to PacketEvents EntityType
        return switch (type) {
            case ZOMBIE -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ZOMBIE;
            case SKELETON -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.SKELETON;
            case CREEPER -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.CREEPER;
            case VILLAGER -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.VILLAGER;
            case IRON_GOLEM -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.IRON_GOLEM;
            case SNOW_GOLEM -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.SNOW_GOLEM;
            case WITCH -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.WITCH;
            case PILLAGER -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.PILLAGER;
            case VINDICATOR -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.VINDICATOR;
            case EVOKER -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.EVOKER;
            case ENDERMAN -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ENDERMAN;
            case PIGLIN -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.PIGLIN;
            case PIGLIN_BRUTE -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.PIGLIN_BRUTE;
            case ZOMBIFIED_PIGLIN -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ZOMBIFIED_PIGLIN;
            case BLAZE -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.BLAZE;
            case WITHER_SKELETON -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.WITHER_SKELETON;
            case GUARDIAN -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.GUARDIAN;
            case ELDER_GUARDIAN -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ELDER_GUARDIAN;
            case SHULKER -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.SHULKER;
            case WARDEN -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.WARDEN;
            case ALLAY -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ALLAY;
            case VEX -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.VEX;
            case COW -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.COW;
            case PIG -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.PIG;
            case SHEEP -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.SHEEP;
            case CHICKEN -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.CHICKEN;
            case WOLF -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.WOLF;
            case CAT -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.CAT;
            case HORSE -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.HORSE;
            case DONKEY -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.DONKEY;
            case MULE -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.MULE;
            case LLAMA -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.LLAMA;
            case FOX -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.FOX;
            case PANDA -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.PANDA;
            case BEE -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.BEE;
            case FROG -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.FROG;
            case AXOLOTL -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.AXOLOTL;
            case GOAT -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.GOAT;
            case CAMEL -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.CAMEL;
            case SNIFFER -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.SNIFFER;
            case ARMADILLO -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ARMADILLO;
            default -> throw new UnsupportedOperationException("Entity type not supported: " + type);
        };
    }

    private com.github.retrooper.packetevents.protocol.entity.pose.EntityPose convertPose(
            LivingAppearance.EntityPose pose) {
        return switch (pose) {
            case STANDING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.STANDING;
            case FALL_FLYING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.FALL_FLYING;
            case SLEEPING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.SLEEPING;
            case SWIMMING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.SWIMMING;
            case SPIN_ATTACK -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.SPIN_ATTACK;
            case SNEAKING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.CROUCHING;
            case DYING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.DYING;
            default -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.STANDING;
        };
    }

    /**
     * Gets the EntityLib wrapper entity.
     *
     * @return the wrapper entity
     */
    public WrapperLivingEntity getWrapperEntity() {
        return wrapperEntity;
    }
}
