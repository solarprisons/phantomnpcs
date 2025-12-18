package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Packet for updating entity metadata.
 *
 * @param entityId the entity ID
 * @param entries  the metadata entries
 */
public record EntityMetadataPacket(
        int entityId,
        @NotNull List<Entry> entries
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return PacketType.ENTITY_METADATA;
    }

    /**
     * Creates a metadata packet with a single entry.
     *
     * @param entityId entity ID
     * @param index    metadata index
     * @param type     metadata type
     * @param value    metadata value
     * @return the packet
     */
    public static EntityMetadataPacket of(int entityId, int index, MetadataType type, Object value) {
        return new EntityMetadataPacket(entityId, List.of(new Entry(index, type, value)));
    }

    /**
     * A metadata entry.
     *
     * @param index the metadata index
     * @param type  the metadata type
     * @param value the value
     */
    public record Entry(
            int index,
            @NotNull MetadataType type,
            Object value
    ) {}

    /**
     * Metadata value types.
     */
    public enum MetadataType {
        BYTE,
        VAR_INT,
        VAR_LONG,
        FLOAT,
        STRING,
        CHAT,
        OPTIONAL_CHAT,
        ITEM_STACK,
        BOOLEAN,
        ROTATIONS,
        POSITION,
        OPTIONAL_POSITION,
        DIRECTION,
        OPTIONAL_UUID,
        BLOCK_STATE,
        OPTIONAL_BLOCK_STATE,
        NBT,
        PARTICLE,
        PARTICLES,
        VILLAGER_DATA,
        OPTIONAL_UNSIGNED_INT,
        POSE,
        CAT_VARIANT,
        WOLF_VARIANT,
        FROG_VARIANT,
        OPTIONAL_GLOBAL_POSITION,
        PAINTING_VARIANT,
        SNIFFER_STATE,
        ARMADILLO_STATE,
        VECTOR3,
        QUATERNION
    }
}
