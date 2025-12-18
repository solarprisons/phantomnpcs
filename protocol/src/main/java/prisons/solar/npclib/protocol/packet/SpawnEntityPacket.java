package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.entity.EntityType;

import java.util.UUID;

/**
 * Packet for spawning a non-player entity.
 *
 * @param entityId   the entity ID
 * @param uuid       the entity UUID
 * @param entityType the entity type
 * @param x          x position
 * @param y          y position
 * @param z          z position
 * @param yaw        yaw rotation
 * @param pitch      pitch rotation
 * @param headYaw    head yaw rotation
 * @param data       entity-specific data
 * @param velocityX  x velocity
 * @param velocityY  y velocity
 * @param velocityZ  z velocity
 */
public record SpawnEntityPacket(
        int entityId,
        @NotNull UUID uuid,
        @NotNull EntityType entityType,
        double x,
        double y,
        double z,
        byte yaw,
        byte pitch,
        byte headYaw,
        int data,
        short velocityX,
        short velocityY,
        short velocityZ
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return PacketType.SPAWN_ENTITY;
    }

    /**
     * Creates a spawn entity packet.
     *
     * @param entityId   entity ID
     * @param uuid       entity UUID
     * @param entityType entity type
     * @param x          x position
     * @param y          y position
     * @param z          z position
     * @param yaw        yaw in degrees
     * @param pitch      pitch in degrees
     * @return the packet
     */
    public static SpawnEntityPacket of(int entityId, UUID uuid, EntityType entityType,
                                       double x, double y, double z, float yaw, float pitch) {
        return new SpawnEntityPacket(
                entityId,
                uuid,
                entityType,
                x, y, z,
                toProtocolAngle(yaw),
                toProtocolAngle(pitch),
                toProtocolAngle(yaw),
                0,
                (short) 0, (short) 0, (short) 0
        );
    }

    private static byte toProtocolAngle(float degrees) {
        return (byte) (degrees * 256.0f / 360.0f);
    }
}
