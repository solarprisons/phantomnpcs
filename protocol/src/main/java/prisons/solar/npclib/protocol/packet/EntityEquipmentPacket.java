package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Packet for updating entity equipment.
 *
 * @param entityId  the entity ID
 * @param equipment the equipment entries
 */
public record EntityEquipmentPacket(
        int entityId,
        @NotNull List<Entry> equipment
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return PacketType.ENTITY_EQUIPMENT;
    }

    /**
     * Creates an equipment packet for a single slot.
     *
     * @param entityId entity ID
     * @param slot     equipment slot
     * @param item     item data (platform-specific)
     * @return the packet
     */
    public static EntityEquipmentPacket of(int entityId, @NotNull Slot slot, @Nullable Object item) {
        return new EntityEquipmentPacket(entityId, List.of(new Entry(slot, item)));
    }

    /**
     * Equipment slot.
     */
    public enum Slot {
        MAIN_HAND(0),
        OFF_HAND(1),
        FEET(2),
        LEGS(3),
        CHEST(4),
        HEAD(5);

        private final int id;

        Slot(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }
    }

    /**
     * Equipment entry.
     *
     * @param slot the slot
     * @param item the item (platform-specific, null for empty)
     */
    public record Entry(
            @NotNull Slot slot,
            @Nullable Object item
    ) {}
}
