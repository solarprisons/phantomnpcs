package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;

/**
 * Packet for rotating an entity's head.
 *
 * @param entityId the entity ID
 * @param headYaw  the head yaw angle
 */
public record EntityHeadRotationPacket(
        int entityId,
        byte headYaw
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return PacketType.ENTITY_HEAD_ROTATION;
    }

    /**
     * Creates a head rotation packet.
     *
     * @param entityId entity ID
     * @param yaw      yaw in degrees
     * @return the packet
     */
    public static EntityHeadRotationPacket of(int entityId, float yaw) {
        return new EntityHeadRotationPacket(entityId, toProtocolAngle(yaw));
    }

    private static byte toProtocolAngle(float degrees) {
        return (byte) (degrees * 256.0f / 360.0f);
    }
}
