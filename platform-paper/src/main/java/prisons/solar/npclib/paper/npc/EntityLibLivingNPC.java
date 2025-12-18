package prisons.solar.npclib.paper.npc;

import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
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

public class EntityLibLivingNPC extends AbstractNPC<LivingAppearance> {

    private WrapperLivingEntity wrapperEntity;
    private final EntityLibLivingAppearance livingAppearance;
    private SimpleEventBus eventBus;

    public EntityLibLivingNPC(@NotNull EntityType entityType, @NotNull Position position) {
        super(entityType, position);
        this.livingAppearance = new EntityLibLivingAppearance(this);
        this.appearance = livingAppearance;
    }

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

    public void syncEquipment() {
        if (wrapperEntity == null) {
            return;
        }

        var equipment = wrapperEntity.getEquipment();

        for (LivingAppearance.EquipmentSlot slot : LivingAppearance.EquipmentSlot.values()) {
            Object item = livingAppearance.getEquipment(slot);
            if (item instanceof org.bukkit.inventory.ItemStack bukkitItem) {
                ItemStack peItem = SpigotConversionUtil.fromBukkitItemStack(bukkitItem);

                EquipmentSlot peSlot = switch (slot) {
                    case MAIN_HAND -> EquipmentSlot.MAIN_HAND;
                    case OFF_HAND -> EquipmentSlot.OFF_HAND;
                    case HEAD -> EquipmentSlot.HELMET;
                    case CHEST -> EquipmentSlot.CHEST_PLATE;
                    case LEGS -> EquipmentSlot.LEGGINGS;
                    case FEET -> EquipmentSlot.BOOTS;
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

    private void ensureWrapperEntity() {
        if (wrapperEntity == null) {
            com.github.retrooper.packetevents.protocol.entity.type.EntityType peType = convertEntityType(entityType);
            wrapperEntity = new WrapperLivingEntity(entityId, id, peType);
            syncMetadataToWrapper();
        }
    }

    private com.github.retrooper.packetevents.protocol.entity.type.EntityType convertEntityType(EntityType type) {
        return switch (type) {
            case ZOMBIE -> EntityTypes.ZOMBIE;
            case SKELETON -> EntityTypes.SKELETON;
            case CREEPER -> EntityTypes.CREEPER;
            case VILLAGER -> EntityTypes.VILLAGER;
            case IRON_GOLEM -> EntityTypes.IRON_GOLEM;
            case SNOW_GOLEM -> EntityTypes.SNOW_GOLEM;
            case WITCH -> EntityTypes.WITCH;
            case PILLAGER -> EntityTypes.PILLAGER;
            case VINDICATOR -> EntityTypes.VINDICATOR;
            case EVOKER -> EntityTypes.EVOKER;
            case ENDERMAN -> EntityTypes.ENDERMAN;
            case PIGLIN -> EntityTypes.PIGLIN;
            case PIGLIN_BRUTE -> EntityTypes.PIGLIN_BRUTE;
            case ZOMBIFIED_PIGLIN -> EntityTypes.ZOMBIFIED_PIGLIN;
            case BLAZE -> EntityTypes.BLAZE;
            case WITHER_SKELETON -> EntityTypes.WITHER_SKELETON;
            case GUARDIAN -> EntityTypes.GUARDIAN;
            case ELDER_GUARDIAN -> EntityTypes.ELDER_GUARDIAN;
            case SHULKER -> EntityTypes.SHULKER;
            case WARDEN -> EntityTypes.WARDEN;
            case ALLAY -> EntityTypes.ALLAY;
            case VEX -> EntityTypes.VEX;
            case COW -> EntityTypes.COW;
            case PIG -> EntityTypes.PIG;
            case SHEEP -> EntityTypes.SHEEP;
            case CHICKEN -> EntityTypes.CHICKEN;
            case WOLF -> EntityTypes.WOLF;
            case CAT -> EntityTypes.CAT;
            case HORSE -> EntityTypes.HORSE;
            case DONKEY -> EntityTypes.DONKEY;
            case MULE -> EntityTypes.MULE;
            case LLAMA -> EntityTypes.LLAMA;
            case FOX -> EntityTypes.FOX;
            case PANDA -> EntityTypes.PANDA;
            case BEE -> EntityTypes.BEE;
            case FROG -> EntityTypes.FROG;
            case AXOLOTL -> EntityTypes.AXOLOTL;
            case GOAT -> EntityTypes.GOAT;
            case CAMEL -> EntityTypes.CAMEL;
            case SNIFFER -> EntityTypes.SNIFFER;
            case ARMADILLO -> EntityTypes.ARMADILLO;
            default -> throw new UnsupportedOperationException("Entity type not supported: " + type);
        };
    }

    private EntityPose convertPose(LivingAppearance.EntityPose pose) {
        return switch (pose) {
            case STANDING -> EntityPose.STANDING;
            case FALL_FLYING -> EntityPose.FALL_FLYING;
            case SLEEPING -> EntityPose.SLEEPING;
            case SWIMMING -> EntityPose.SWIMMING;
            case SPIN_ATTACK -> EntityPose.SPIN_ATTACK;
            case SNEAKING -> EntityPose.CROUCHING;
            case DYING -> EntityPose.DYING;
            default -> EntityPose.STANDING;
        };
    }

    public WrapperLivingEntity getWrapperEntity() {
        return wrapperEntity;
    }
}
