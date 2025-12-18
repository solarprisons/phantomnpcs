package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an outbound packet to be sent to a viewer.
 * Implementations are protocol-version agnostic.
 */
public interface OutboundPacket {

    /**
     * Gets the type of this packet.
     *
     * @return the packet type
     */
    @NotNull PacketType type();
}
