package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;

/**
 * Packet for playing entity animations.
 *
 * <p>This packet is used for standard animation types that work across entity types.
 * For entity-specific animations, use {@link EntityStatusPacket} instead.
 *
 * <p>Standard animation IDs:
 * <ul>
 *   <li>0 - Swing main arm</li>
 *   <li>1 - Take damage</li>
 *   <li>2 - Leave bed</li>
 *   <li>3 - Swing offhand</li>
 *   <li>4 - Critical effect</li>
 *   <li>5 - Magic critical effect</li>
 * </ul>
 *
 * @param entityId  the entity ID
 * @param animation the animation ID (0-5)
 */
public record EntityAnimationPacket(
        int entityId,
        int animation
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return PacketType.ENTITY_ANIMATION;
    }
}
