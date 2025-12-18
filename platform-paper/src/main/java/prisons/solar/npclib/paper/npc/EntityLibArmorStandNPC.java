package prisons.solar.npclib.paper.npc;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.util.Vector3f;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.other.ArmorStandMeta;
import me.tofaa.entitylib.wrapper.WrapperLivingEntity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.ArmorStandAppearance;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.npc.NPCState;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.core.event.SimpleEventBus;
import prisons.solar.npclib.core.event.SimpleNPCDespawnEvent;
import prisons.solar.npclib.core.event.SimpleNPCSpawnEvent;
import prisons.solar.npclib.core.npc.AbstractNPC;
import prisons.solar.npclib.paper.PaperViewer;

public class EntityLibArmorStandNPC extends AbstractNPC<ArmorStandAppearance> {

    private WrapperLivingEntity wrapperEntity;
    private final EntityLibArmorStandAppearance armorStandAppearance;
    private SimpleEventBus eventBus;

    public EntityLibArmorStandNPC(@NotNull Position position) {
        super(EntityType.ARMOR_STAND, position);
        this.armorStandAppearance = new EntityLibArmorStandAppearance(this);
        this.appearance = armorStandAppearance;
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
    }

    @Override
    public void playAnimation(@NotNull Viewer viewer, @NotNull Animation animation) {
    }

    @Override
    protected void onMetadataChanged() {
        syncMetadataToWrapper();
    }

    public void syncMetadataToWrapper() {
        if (wrapperEntity == null) {
            return;
        }

        wrapperEntity.consumeEntityMeta(ArmorStandMeta.class, meta -> {
            meta.setOnFire(armorStandAppearance.isOnFire());
            meta.setInvisible(armorStandAppearance.isInvisible());
            meta.setGlowing(armorStandAppearance.isGlowing());

            if (!armorStandAppearance.getCustomName().isEmpty()) {
                meta.setCustomName(Component.text(armorStandAppearance.getCustomName()));
                meta.setCustomNameVisible(armorStandAppearance.isCustomNameVisible());
            }

            meta.setSmall(armorStandAppearance.isSmall());
            meta.setHasArms(armorStandAppearance.hasArms());
            meta.setHasNoBasePlate(!armorStandAppearance.hasBasePlate());
            meta.setMarker(armorStandAppearance.isMarker());

            ArmorStandAppearance.Rotation head = armorStandAppearance.getHeadRotation();
            meta.setHeadRotation(new Vector3f(head.pitch(), head.yaw(), head.roll()));

            ArmorStandAppearance.Rotation body = armorStandAppearance.getBodyRotation();
            meta.setBodyRotation(new Vector3f(body.pitch(), body.yaw(), body.roll()));

            ArmorStandAppearance.Rotation leftArm = armorStandAppearance.getLeftArmRotation();
            meta.setLeftArmRotation(new Vector3f(leftArm.pitch(), leftArm.yaw(), leftArm.roll()));

            ArmorStandAppearance.Rotation rightArm = armorStandAppearance.getRightArmRotation();
            meta.setRightArmRotation(new Vector3f(rightArm.pitch(), rightArm.yaw(), rightArm.roll()));

            ArmorStandAppearance.Rotation leftLeg = armorStandAppearance.getLeftLegRotation();
            meta.setLeftLegRotation(new Vector3f(leftLeg.pitch(), leftLeg.yaw(), leftLeg.roll()));

            ArmorStandAppearance.Rotation rightLeg = armorStandAppearance.getRightLegRotation();
            meta.setRightLegRotation(new Vector3f(rightLeg.pitch(), rightLeg.yaw(), rightLeg.roll()));
        });

        wrapperEntity.refresh();
    }

    public void syncEquipment() {
        if (wrapperEntity == null) {
            return;
        }

        var equipment = wrapperEntity.getEquipment();

        for (ArmorStandAppearance.EquipmentSlot slot : ArmorStandAppearance.EquipmentSlot.values()) {
            Object item = armorStandAppearance.getEquipment(slot);
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
            wrapperEntity = new WrapperLivingEntity(entityId, id, EntityTypes.ARMOR_STAND);
            syncMetadataToWrapper();
        }
    }

    public WrapperLivingEntity getWrapperEntity() {
        return wrapperEntity;
    }
}
