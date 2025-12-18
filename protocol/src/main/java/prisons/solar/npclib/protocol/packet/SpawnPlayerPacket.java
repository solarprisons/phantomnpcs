package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Packet for spawning a player entity.
 *
 * @param entityId the entity ID
 * @param uuid     the player UUID
 * @param x        x position
 * @param y        y position
 * @param z        z position
 * @param yaw      yaw rotation (0-255 mapped to 0-360)
 * @param pitch    pitch rotation (0-255 mapped to 0-360)
 */
public record SpawnPlayerPacket(
        int entityId,
        @NotNull UUID uuid,
        double x,
        double y,
        double z,
        byte yaw,
        byte pitch
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return PacketType.SPAWN_PLAYER;
    }

    /**
     * Creates a spawn player packet from coordinates and rotation.
     *
     * @param entityId entity ID
     * @param uuid     player UUID
     * @param x        x position
     * @param y        y position
     * @param z        z position
     * @param yaw      yaw in degrees
     * @param pitch    pitch in degrees
     * @return the packet
     */
    public static SpawnPlayerPacket of(int entityId, UUID uuid, double x, double y, double z, float yaw, float pitch) {
        return new SpawnPlayerPacket(
                entityId,
                uuid,
                x,
                y,
                z,
                toProtocolAngle(yaw),
                toProtocolAngle(pitch)
        );
    }

    private static byte toProtocolAngle(float degrees) {
        return (byte) (degrees * 256.0f / 360.0f);
    }
}
