package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;

/**
 * Packet for destroying (despawning) entities.
 *
 * @param entityIds the entity IDs to destroy
 */
public record EntityDestroyPacket(
        int @NotNull [] entityIds
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return PacketType.ENTITY_DESTROY;
    }

    /**
     * Creates a destroy packet for a single entity.
     *
     * @param entityId the entity ID
     * @return the packet
     */
    public static EntityDestroyPacket of(int entityId) {
        return new EntityDestroyPacket(new int[]{entityId});
    }

    /**
     * Creates a destroy packet for multiple entities.
     *
     * @param entityIds the entity IDs
     * @return the packet
     */
    public static EntityDestroyPacket of(int... entityIds) {
        return new EntityDestroyPacket(entityIds.clone());
    }
}
