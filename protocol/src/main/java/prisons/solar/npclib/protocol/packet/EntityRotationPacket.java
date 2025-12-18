package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;

/**
 * Packet for rotating an entity (body rotation, no position change).
 *
 * @param entityId the entity ID
 * @param yaw      yaw rotation
 * @param pitch    pitch rotation
 * @param onGround whether on ground
 */
public record EntityRotationPacket(
        int entityId,
        byte yaw,
        byte pitch,
        boolean onGround
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return PacketType.ENTITY_LOOK;
    }

    /**
     * Creates a rotation packet.
     *
     * @param entityId entity ID
     * @param yaw      yaw in degrees
     * @param pitch    pitch in degrees
     * @return the packet
     */
    public static EntityRotationPacket of(int entityId, float yaw, float pitch) {
        return new EntityRotationPacket(
                entityId,
                toProtocolAngle(yaw),
                toProtocolAngle(pitch),
                true
        );
    }

    private static byte toProtocolAngle(float degrees) {
        return (byte) (degrees * 256.0f / 360.0f);
    }
}
