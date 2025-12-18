package prisons.solar.npclib.protocol.adapter;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.protocol.packet.OutboundPacket;
import prisons.solar.npclib.protocol.packet.PacketType;

import java.util.List;

/**
 * Adapter for sending packets to viewers.
 * Implementations translate abstract packets to protocol-specific packets.
 */
public interface ProtocolAdapter {

    /**
     * Gets the name of this protocol adapter.
     *
     * @return adapter name
     */
    @NotNull String name();

    /**
     * Gets the priority of this adapter.
     * Higher priority adapters are preferred when multiple are available.
     *
     * @return priority value
     */
    int priority();

    /**
     * Initializes this adapter.
     */
    void initialize();

    /**
     * Shuts down this adapter.
     */
    void shutdown();

    /**
     * Sends a packet to a viewer.
     *
     * @param viewer the viewer
     * @param packet the packet to send
     */
    void send(@NotNull Viewer viewer, @NotNull OutboundPacket packet);

    /**
     * Sends multiple packets to a viewer.
     *
     * @param viewer  the viewer
     * @param packets the packets to send
     */
    default void sendBatch(@NotNull Viewer viewer, @NotNull List<OutboundPacket> packets) {
        for (OutboundPacket packet : packets) {
            send(viewer, packet);
        }
    }

    /**
     * Checks if this adapter supports a packet type.
     *
     * @param type the packet type
     * @return true if supported
     */
    boolean supports(@NotNull PacketType type);

    /**
     * Gets the protocol version this adapter is running on.
     *
     * @return protocol version number
     */
    int protocolVersion();

    /**
     * Registers an interaction listener for receiving USE_ENTITY packets.
     *
     * @param listener the listener
     */
    void registerInteractionListener(@NotNull InteractionListener listener);

    /**
     * Listener for entity interactions.
     */
    @FunctionalInterface
    interface InteractionListener {
        /**
         * Called when a viewer interacts with an entity.
         *
         * @param viewer   the viewer
         * @param entityId the entity ID
         * @param action   the interaction action
         */
        void onInteract(@NotNull Viewer viewer, int entityId, @NotNull InteractionAction action);
    }

    /**
     * Types of entity interactions.
     */
    enum InteractionAction {
        ATTACK,
        INTERACT,
        INTERACT_AT
    }
}
