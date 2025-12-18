package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;

/**
 * Packet for teleporting an entity to an absolute position.
 *
 * @param entityId the entity ID
 * @param x        x position
 * @param y        y position
 * @param z        z position
 * @param yaw      yaw rotation
 * @param pitch    pitch rotation
 * @param onGround whether on ground
 */
public record EntityTeleportPacket(
        int entityId,
        double x,
        double y,
        double z,
        byte yaw,
        byte pitch,
        boolean onGround
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return PacketType.ENTITY_TELEPORT;
    }

    /**
     * Creates a teleport packet.
     *
     * @param entityId entity ID
     * @param x        x position
     * @param y        y position
     * @param z        z position
     * @param yaw      yaw in degrees
     * @param pitch    pitch in degrees
     * @return the packet
     */
    public static EntityTeleportPacket of(int entityId, double x, double y, double z, float yaw, float pitch) {
        return new EntityTeleportPacket(
                entityId,
                x, y, z,
                toProtocolAngle(yaw),
                toProtocolAngle(pitch),
                true
        );
    }

    private static byte toProtocolAngle(float degrees) {
        return (byte) (degrees * 256.0f / 360.0f);
    }
}
