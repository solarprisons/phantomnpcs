package prisons.solar.npclib.api.persistence;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.PlayerAppearance;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.npc.Position;

import java.util.Map;
import java.util.UUID;

/**
 * Serializable data for an NPC.
 *
 * @param id         the NPC UUID
 * @param entityType the entity type
 * @param position   the spawn position
 * @param customName the custom name
 * @param skin       the skin data (for players)
 * @param metadata   additional metadata
 */
public record NPCData(
        @NotNull UUID id,
        @NotNull EntityType entityType,
        @NotNull Position position,
        @Nullable String customName,
        @Nullable PlayerAppearance.Skin skin,
        @NotNull Map<String, Object> metadata
) {

    /**
     * Creates a builder for NPC data.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link NPCData}.
     */
    public static class Builder {
        private UUID id;
        private EntityType entityType;
        private Position position;
        private String customName;
        private PlayerAppearance.Skin skin;
        private Map<String, Object> metadata = Map.of();

        public Builder id(@NotNull UUID id) {
            this.id = id;
            return this;
        }

        public Builder entityType(@NotNull EntityType entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder position(@NotNull Position position) {
            this.position = position;
            return this;
        }

        public Builder customName(@Nullable String customName) {
            this.customName = customName;
            return this;
        }

        public Builder skin(@Nullable PlayerAppearance.Skin skin) {
            this.skin = skin;
            return this;
        }

        public Builder metadata(@NotNull Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public NPCData build() {
            return new NPCData(id, entityType, position, customName, skin, metadata);
        }
    }
}
