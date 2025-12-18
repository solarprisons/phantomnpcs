package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;

/**
 * Packet for playing entity animations.
 *
 * @param entityId  the entity ID
 * @param animation the animation ID
 */
public record EntityAnimationPacket(
        int entityId,
        int animation
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return PacketType.ENTITY_ANIMATION;
    }

    /**
     * Creates an animation packet.
     *
     * @param entityId  entity ID
     * @param animation the animation
     * @return the packet
     */
    public static EntityAnimationPacket of(int entityId, @NotNull NPC.Animation animation) {
        return new EntityAnimationPacket(entityId, toProtocolAnimation(animation));
    }

    private static int toProtocolAnimation(NPC.Animation animation) {
        return switch (animation) {
            case SWING_MAIN_ARM -> 0;
            case TAKE_DAMAGE -> 1;
            case LEAVE_BED -> 2;
            case SWING_OFFHAND -> 3;
            case CRITICAL_EFFECT -> 4;
            case MAGIC_CRITICAL_EFFECT -> 5;
        };
    }
}
