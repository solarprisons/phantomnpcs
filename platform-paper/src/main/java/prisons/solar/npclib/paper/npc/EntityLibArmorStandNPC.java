package prisons.solar.npclib.paper.npc;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Vector3f;
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

/**
 * Armor stand NPC implementation using EntityLib.
 */
public class EntityLibArmorStandNPC extends AbstractNPC<ArmorStandAppearance> {

    private WrapperLivingEntity wrapperEntity;
    private final EntityLibArmorStandAppearance armorStandAppearance;
    private SimpleEventBus eventBus;

    public EntityLibArmorStandNPC(@NotNull Position position) {
        super(EntityType.ARMOR_STAND, position);
        this.armorStandAppearance = new EntityLibArmorStandAppearance(this);
        this.appearance = armorStandAppearance;
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
        // Armor stands don't support animations
    }

    @Override
    public void playAnimation(@NotNull Viewer viewer, @NotNull Animation animation) {
        // Armor stands don't support animations
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

        wrapperEntity.consumeEntityMeta(ArmorStandMeta.class, meta -> {
            // Base entity flags
            meta.setOnFire(armorStandAppearance.isOnFire());
            meta.setInvisible(armorStandAppearance.isInvisible());
            meta.setGlowing(armorStandAppearance.isGlowing());

            // Custom name
            if (!armorStandAppearance.getCustomName().isEmpty()) {
                meta.setCustomName(Component.text(armorStandAppearance.getCustomName()));
                meta.setCustomNameVisible(armorStandAppearance.isCustomNameVisible());
            }

            // Armor stand specific
            meta.setSmall(armorStandAppearance.isSmall());
            meta.setHasArms(armorStandAppearance.hasArms());
            meta.setHasNoBasePlate(!armorStandAppearance.hasBasePlate());
            meta.setMarker(armorStandAppearance.isMarker());

            // Rotations
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

    /**
     * Syncs equipment to all viewers.
     */
    public void syncEquipment() {
        if (wrapperEntity == null) {
            return;
        }

        var equipment = wrapperEntity.getEquipment();

        for (ArmorStandAppearance.EquipmentSlot slot : ArmorStandAppearance.EquipmentSlot.values()) {
            Object item = armorStandAppearance.getEquipment(slot);
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
            var location = new com.github.retrooper.packetevents.protocol.world.Location(
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

    /**
     * Gets the EntityLib wrapper entity.
     *
     * @return the wrapper entity
     */
    public WrapperLivingEntity getWrapperEntity() {
        return wrapperEntity;
    }
}
