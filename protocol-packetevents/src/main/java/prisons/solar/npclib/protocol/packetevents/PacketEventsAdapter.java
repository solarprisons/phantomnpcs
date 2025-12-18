package prisons.solar.npclib.protocol.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.protocol.adapter.ProtocolAdapter;
import prisons.solar.npclib.protocol.packet.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Protocol adapter implementation using PacketEvents.
 */
public class PacketEventsAdapter implements ProtocolAdapter {

    private final List<InteractionListener> interactionListeners = new CopyOnWriteArrayList<>();
    private final Function<Object, Viewer> viewerResolver;
    private PacketListenerAbstract packetListener;

    /**
     * Creates a new PacketEvents adapter.
     *
     * @param viewerResolver function to resolve Viewer from platform player
     */
    public PacketEventsAdapter(@NotNull Function<Object, Viewer> viewerResolver) {
        this.viewerResolver = viewerResolver;
    }

    @Override
    public @NotNull String name() {
        return "PacketEvents";
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public void initialize() {
        packetListener = new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                if (event.getPacketType() == Play.Client.INTERACT_ENTITY) {
                    handleInteraction(event);
                }
            }
        };
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
    }

    @Override
    public void shutdown() {
        if (packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
        }
        interactionListeners.clear();
    }

    private void handleInteraction(PacketReceiveEvent event) {
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        int entityId = wrapper.getEntityId();
        InteractionAction action = switch (wrapper.getAction()) {
            case ATTACK -> InteractionAction.ATTACK;
            case INTERACT -> InteractionAction.INTERACT;
            case INTERACT_AT -> InteractionAction.INTERACT_AT;
        };

        Viewer viewer = viewerResolver.apply(event.getPlayer());
        if (viewer != null) {
            for (InteractionListener listener : interactionListeners) {
                listener.onInteract(viewer, entityId, action);
            }
        }
    }

    @Override
    public void send(@NotNull Viewer viewer, @NotNull OutboundPacket packet) {
        Object platformPlayer = viewer.platformPlayer();
        Object wrapped = translatePacket(packet);
        if (wrapped != null) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(platformPlayer, wrapped);
        }
    }

    @Override
    public boolean supports(@NotNull PacketType type) {
        return switch (type) {
            case SPAWN_PLAYER, SPAWN_ENTITY, ENTITY_DESTROY, PLAYER_INFO_ADD, PLAYER_INFO_REMOVE,
                 ENTITY_TELEPORT, ENTITY_HEAD_ROTATION, ENTITY_METADATA, ENTITY_EQUIPMENT,
                 ENTITY_ANIMATION, ENTITY_LOOK -> true;
            default -> false;
        };
    }

    @Override
    public int protocolVersion() {
        return PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion();
    }

    @Override
    public void registerInteractionListener(@NotNull InteractionListener listener) {
        interactionListeners.add(listener);
    }

    private Object translatePacket(OutboundPacket packet) {
        return switch (packet) {
            case SpawnPlayerPacket p -> translateSpawnPlayer(p);
            case SpawnEntityPacket p -> translateSpawnEntity(p);
            case EntityDestroyPacket p -> translateDestroy(p);
            case PlayerInfoPacket p -> translatePlayerInfo(p);
            case EntityTeleportPacket p -> translateTeleport(p);
            case EntityHeadRotationPacket p -> translateHeadRotation(p);
            case EntityMetadataPacket p -> translateMetadata(p);
            case EntityEquipmentPacket p -> translateEquipment(p);
            case EntityAnimationPacket p -> translateAnimation(p);
            case EntityRotationPacket p -> translateRotation(p);
            default -> null;
        };
    }

    private WrapperPlayServerSpawnEntity translateSpawnPlayer(SpawnPlayerPacket packet) {
        var position = new com.github.retrooper.packetevents.util.Vector3d(packet.x(), packet.y(), packet.z());
        return new WrapperPlayServerSpawnEntity(
                packet.entityId(),
                java.util.Optional.of(packet.uuid()),
                com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.PLAYER,
                position,
                fromProtocolAngle(packet.pitch()),
                fromProtocolAngle(packet.yaw()),
                fromProtocolAngle(packet.yaw()),
                0,
                java.util.Optional.empty()
        );
    }

    private WrapperPlayServerSpawnEntity translateSpawnEntity(SpawnEntityPacket packet) {
        var entityType = mapEntityType(packet.entityType());
        var position = new com.github.retrooper.packetevents.util.Vector3d(packet.x(), packet.y(), packet.z());
        return new WrapperPlayServerSpawnEntity(
                packet.entityId(),
                java.util.Optional.of(packet.uuid()),
                entityType,
                position,
                fromProtocolAngle(packet.pitch()),
                fromProtocolAngle(packet.yaw()),
                fromProtocolAngle(packet.headYaw()),
                packet.data(),
                java.util.Optional.empty()
        );
    }

    private static float fromProtocolAngle(byte angle) {
        return angle * 360.0f / 256.0f;
    }

    private WrapperPlayServerDestroyEntities translateDestroy(EntityDestroyPacket packet) {
        return new WrapperPlayServerDestroyEntities(packet.entityIds());
    }

    private Object translatePlayerInfo(PlayerInfoPacket packet) {
        if (packet.action() == PlayerInfoPacket.Action.REMOVE_PLAYER) {
            List<java.util.UUID> uuids = packet.entries().stream()
                    .map(PlayerInfoPacket.Entry::uuid)
                    .toList();
            return new WrapperPlayServerPlayerInfoRemove(uuids);
        }

        // Add player
        List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> infos = new ArrayList<>();
        for (PlayerInfoPacket.Entry entry : packet.entries()) {
            List<TextureProperty> textures = new ArrayList<>();
            if (entry.textures() != null) {
                textures.add(new TextureProperty("textures", entry.textures().value(), entry.textures().signature()));
            }

            UserProfile profile = new UserProfile(entry.uuid(), entry.name(), textures);
            WrapperPlayServerPlayerInfoUpdate.PlayerInfo info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                    profile,
                    entry.listed(),
                    entry.latency(),
                    com.github.retrooper.packetevents.protocol.player.GameMode.SURVIVAL,
                    null,
                    null
            );
            infos.add(info);
        }

        return new WrapperPlayServerPlayerInfoUpdate(
                EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                        WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED),
                infos
        );
    }

    private WrapperPlayServerEntityTeleport translateTeleport(EntityTeleportPacket packet) {
        var position = new com.github.retrooper.packetevents.util.Vector3d(packet.x(), packet.y(), packet.z());
        return new WrapperPlayServerEntityTeleport(
                packet.entityId(),
                position,
                fromProtocolAngle(packet.yaw()),
                fromProtocolAngle(packet.pitch()),
                packet.onGround()
        );
    }

    private WrapperPlayServerEntityHeadLook translateHeadRotation(EntityHeadRotationPacket packet) {
        return new WrapperPlayServerEntityHeadLook(packet.entityId(), packet.headYaw());
    }

    private WrapperPlayServerEntityMetadata translateMetadata(EntityMetadataPacket packet) {
        List<com.github.retrooper.packetevents.protocol.entity.data.EntityData> data = new ArrayList<>();
        for (EntityMetadataPacket.Entry entry : packet.entries()) {
            // Simplified - full implementation would map all metadata types
            var entityData = createEntityData(entry);
            if (entityData != null) {
                data.add(entityData);
            }
        }
        return new WrapperPlayServerEntityMetadata(packet.entityId(), data);
    }

    private com.github.retrooper.packetevents.protocol.entity.data.EntityData createEntityData(EntityMetadataPacket.Entry entry) {
        var dataTypes = com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.class;
        return switch (entry.type()) {
            case BYTE -> new com.github.retrooper.packetevents.protocol.entity.data.EntityData(
                    entry.index(),
                    com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BYTE,
                    entry.value()
            );
            case BOOLEAN -> new com.github.retrooper.packetevents.protocol.entity.data.EntityData(
                    entry.index(),
                    com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.BOOLEAN,
                    entry.value()
            );
            case STRING -> new com.github.retrooper.packetevents.protocol.entity.data.EntityData(
                    entry.index(),
                    com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes.STRING,
                    entry.value()
            );
            default -> null;
        };
    }

    private WrapperPlayServerEntityEquipment translateEquipment(EntityEquipmentPacket packet) {
        List<com.github.retrooper.packetevents.protocol.player.Equipment> equipment = new ArrayList<>();
        for (EntityEquipmentPacket.Entry entry : packet.equipment()) {
            var slot = mapEquipmentSlot(entry.slot());
            // Item needs to be converted from platform-specific to PacketEvents ItemStack
            var item = entry.item() != null
                    ? com.github.retrooper.packetevents.protocol.item.ItemStack.builder().build()
                    : com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY;
            equipment.add(new com.github.retrooper.packetevents.protocol.player.Equipment(slot, item));
        }
        return new WrapperPlayServerEntityEquipment(packet.entityId(), equipment);
    }

    private WrapperPlayServerEntityAnimation translateAnimation(EntityAnimationPacket packet) {
        var type = com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType.values()[packet.animation()];
        return new WrapperPlayServerEntityAnimation(packet.entityId(), type);
    }

    private WrapperPlayServerEntityRotation translateRotation(EntityRotationPacket packet) {
        return new WrapperPlayServerEntityRotation(
                packet.entityId(),
                packet.yaw(),
                packet.pitch(),
                packet.onGround()
        );
    }

    private com.github.retrooper.packetevents.protocol.entity.type.EntityType mapEntityType(prisons.solar.npclib.api.entity.EntityType type) {
        return switch (type) {
            case PLAYER -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.PLAYER;
            case ZOMBIE -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ZOMBIE;
            case SKELETON -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.SKELETON;
            case VILLAGER -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.VILLAGER;
            case ARMOR_STAND -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ARMOR_STAND;
            case TEXT_DISPLAY -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.TEXT_DISPLAY;
            case BLOCK_DISPLAY -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.BLOCK_DISPLAY;
            case ITEM_DISPLAY -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ITEM_DISPLAY;
            default -> com.github.retrooper.packetevents.protocol.entity.type.EntityTypes.ZOMBIE;
        };
    }

    private com.github.retrooper.packetevents.protocol.player.EquipmentSlot mapEquipmentSlot(EntityEquipmentPacket.Slot slot) {
        return switch (slot) {
            case MAIN_HAND -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.MAIN_HAND;
            case OFF_HAND -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.OFF_HAND;
            case FEET -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.BOOTS;
            case LEGS -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.LEGGINGS;
            case CHEST -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.CHEST_PLATE;
            case HEAD -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.HELMET;
        };
    }
}
