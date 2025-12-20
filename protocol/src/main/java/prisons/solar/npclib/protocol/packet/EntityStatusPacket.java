package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;

/**
 * Packet for sending entity status codes.
 *
 * <p>Entity status packets are used for entity-specific animations and effects
 * that cannot be represented by standard animation packets. Each entity type
 * supports different status codes.
 *
 * @param entityId the entity ID
 * @param status   the status code
 * @see <a href="https://minecraft.wiki/w/Java_Edition_protocol/Entity_statuses">Entity Statuses</a>
 */
public record EntityStatusPacket(
        int entityId,
        byte status
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return PacketType.ENTITY_STATUS;
    }
}
