package prisons.solar.npclib.protocol.packet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Packet for player info (tablist) updates.
 *
 * @param action  the action type
 * @param entries the player entries
 */
public record PlayerInfoPacket(
        @NotNull Action action,
        @NotNull List<Entry> entries
) implements OutboundPacket {

    @Override
    public @NotNull PacketType type() {
        return switch (action) {
            case ADD_PLAYER -> PacketType.PLAYER_INFO_ADD;
            case REMOVE_PLAYER -> PacketType.PLAYER_INFO_REMOVE;
            default -> PacketType.PLAYER_INFO_UPDATE;
        };
    }

    /**
     * Creates an add player packet.
     *
     * @param entry the player entry
     * @return the packet
     */
    public static PlayerInfoPacket addPlayer(@NotNull Entry entry) {
        return new PlayerInfoPacket(Action.ADD_PLAYER, List.of(entry));
    }

    /**
     * Creates a remove player packet.
     *
     * @param uuid the player UUID
     * @return the packet
     */
    public static PlayerInfoPacket removePlayer(@NotNull UUID uuid) {
        return new PlayerInfoPacket(Action.REMOVE_PLAYER, List.of(new Entry(uuid, null, null, 0, 0, null, false)));
    }

    /**
     * Player info actions.
     */
    public enum Action {
        ADD_PLAYER,
        INITIALIZE_CHAT,
        UPDATE_GAME_MODE,
        UPDATE_LISTED,
        UPDATE_LATENCY,
        UPDATE_DISPLAY_NAME,
        REMOVE_PLAYER
    }

    /**
     * Player info entry.
     *
     * @param uuid        player UUID
     * @param name        player name (for ADD)
     * @param textures    skin textures (for ADD)
     * @param gameMode    game mode (for ADD/UPDATE)
     * @param latency     latency in ms (for ADD/UPDATE)
     * @param displayName display name (for ADD/UPDATE)
     * @param listed      whether listed in tablist (for ADD/UPDATE)
     */
    public record Entry(
            @NotNull UUID uuid,
            @Nullable String name,
            @Nullable Textures textures,
            int gameMode,
            int latency,
            @Nullable String displayName,
            boolean listed
    ) {
        /**
         * Creates a basic entry for adding a player NPC.
         *
         * @param uuid      player UUID
         * @param name      player name
         * @param texture   skin texture
         * @param signature skin signature
         * @return the entry
         */
        public static Entry create(@NotNull UUID uuid, @NotNull String name,
                                   @Nullable String texture, @Nullable String signature) {
            Textures textures = texture != null ? new Textures(texture, signature) : null;
            return new Entry(uuid, name, textures, 0, 0, null, false);
        }
    }

    /**
     * Skin textures.
     *
     * @param value     base64 texture value
     * @param signature optional signature
     */
    public record Textures(
            @NotNull String value,
            @Nullable String signature
    ) {}
}
