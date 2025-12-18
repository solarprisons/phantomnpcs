package prisons.solar.npclib.core.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.appearance.PlayerAppearance;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.persistence.NPCData;
import prisons.solar.npclib.api.persistence.NPCPersistence;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JSON file-based implementation of {@link NPCPersistence}.
 */
public class JsonNPCPersistence implements NPCPersistence {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Phantom-Persistence");
        t.setDaemon(true);
        return t;
    });

    private final Path directory;
    private final Gson gson;

    public JsonNPCPersistence(@NotNull Path directory) {
        this.directory = directory;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Position.class, new PositionAdapter())
                .registerTypeAdapter(PlayerAppearance.Skin.class, new SkinAdapter())
                .create();

        // Ensure directory exists
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create NPC storage directory", e);
        }
    }

    @Override
    public CompletableFuture<Void> save(@NotNull NPC<?> npc) {
        return CompletableFuture.runAsync(() -> {
            NPCData data = toData(npc);
            Path file = directory.resolve(npc.id() + ".json");

            try {
                String json = gson.toJson(new SerializableNPCData(data));
                Files.writeString(file, json);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save NPC: " + npc.id(), e);
            }
        }, EXECUTOR);
    }

    @Override
    public CompletableFuture<Void> saveAll(@NotNull Collection<NPC<?>> npcs) {
        return CompletableFuture.runAsync(() -> {
            for (NPC<?> npc : npcs) {
                NPCData data = toData(npc);
                Path file = directory.resolve(npc.id() + ".json");

                try {
                    String json = gson.toJson(new SerializableNPCData(data));
                    Files.writeString(file, json);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save NPC: " + npc.id(), e);
                }
            }
        }, EXECUTOR);
    }

    @Override
    public CompletableFuture<Optional<NPCData>> load(@NotNull UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = directory.resolve(id + ".json");

            if (!Files.exists(file)) {
                return Optional.empty();
            }

            try {
                String json = Files.readString(file);
                SerializableNPCData serializable = gson.fromJson(json, SerializableNPCData.class);
                return Optional.of(serializable.toNPCData());
            } catch (IOException e) {
                throw new RuntimeException("Failed to load NPC: " + id, e);
            }
        }, EXECUTOR);
    }

    @Override
    public CompletableFuture<Collection<NPCData>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<NPCData> result = new ArrayList<>();

            try {
                if (!Files.exists(directory)) {
                    return result;
                }

                Files.list(directory)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(file -> {
                            try {
                                String json = Files.readString(file);
                                SerializableNPCData serializable = gson.fromJson(json, SerializableNPCData.class);
                                result.add(serializable.toNPCData());
                            } catch (IOException e) {
                                // Log and continue
                                System.err.println("Failed to load NPC from " + file + ": " + e.getMessage());
                            }
                        });
            } catch (IOException e) {
                throw new RuntimeException("Failed to list NPC files", e);
            }

            return result;
        }, EXECUTOR);
    }

    @Override
    public CompletableFuture<Void> delete(@NotNull UUID id) {
        return CompletableFuture.runAsync(() -> {
            Path file = directory.resolve(id + ".json");

            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete NPC: " + id, e);
            }
        }, EXECUTOR);
    }

    @Override
    public CompletableFuture<Boolean> exists(@NotNull UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            Path file = directory.resolve(id + ".json");
            return Files.exists(file);
        }, EXECUTOR);
    }

    private NPCData toData(NPC<?> npc) {
        NPCData.Builder builder = NPCData.builder()
                .id(npc.id())
                .entityType(npc.entityType())
                .position(npc.position())
                .customName(npc.appearance().getCustomName());

        // Add skin for player NPCs
        if (npc.appearance() instanceof PlayerAppearance playerAppearance) {
            builder.skin(playerAppearance.getSkin());
        }

        return builder.build();
    }

    /**
     * Serializable wrapper for NPCData.
     */
    private static class SerializableNPCData {
        String id;
        String entityType;
        String worldId;
        double x, y, z;
        float yaw, pitch;
        String customName;
        String skinTexture;
        String skinSignature;
        Map<String, Object> metadata;

        SerializableNPCData() {}

        SerializableNPCData(NPCData data) {
            this.id = data.id().toString();
            this.entityType = data.entityType().name();
            this.worldId = data.position().worldId();
            this.x = data.position().x();
            this.y = data.position().y();
            this.z = data.position().z();
            this.yaw = data.position().yaw();
            this.pitch = data.position().pitch();
            this.customName = data.customName();

            if (data.skin() != null) {
                this.skinTexture = data.skin().texture();
                this.skinSignature = data.skin().signature();
            }

            this.metadata = data.metadata();
        }

        NPCData toNPCData() {
            PlayerAppearance.Skin skin = null;
            if (skinTexture != null) {
                skin = PlayerAppearance.Skin.of(skinTexture, skinSignature);
            }

            return NPCData.builder()
                    .id(UUID.fromString(id))
                    .entityType(EntityType.valueOf(entityType))
                    .position(Position.of(worldId, x, y, z, yaw, pitch))
                    .customName(customName)
                    .skin(skin)
                    .metadata(metadata != null ? metadata : Map.of())
                    .build();
        }
    }

    /**
     * Gson adapter for Position.
     */
    private static class PositionAdapter implements com.google.gson.JsonSerializer<Position>,
            com.google.gson.JsonDeserializer<Position> {

        @Override
        public com.google.gson.JsonElement serialize(Position src, Type typeOfSrc,
                                                      com.google.gson.JsonSerializationContext context) {
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("world", src.worldId());
            obj.addProperty("x", src.x());
            obj.addProperty("y", src.y());
            obj.addProperty("z", src.z());
            obj.addProperty("yaw", src.yaw());
            obj.addProperty("pitch", src.pitch());
            return obj;
        }

        @Override
        public Position deserialize(com.google.gson.JsonElement json, Type typeOfT,
                                    com.google.gson.JsonDeserializationContext context) {
            com.google.gson.JsonObject obj = json.getAsJsonObject();
            return Position.of(
                    obj.get("world").getAsString(),
                    obj.get("x").getAsDouble(),
                    obj.get("y").getAsDouble(),
                    obj.get("z").getAsDouble(),
                    obj.get("yaw").getAsFloat(),
                    obj.get("pitch").getAsFloat()
            );
        }
    }

    /**
     * Gson adapter for Skin.
     */
    private static class SkinAdapter implements com.google.gson.JsonSerializer<PlayerAppearance.Skin>,
            com.google.gson.JsonDeserializer<PlayerAppearance.Skin> {

        @Override
        public com.google.gson.JsonElement serialize(PlayerAppearance.Skin src, Type typeOfSrc,
                                                      com.google.gson.JsonSerializationContext context) {
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("texture", src.texture());
            obj.addProperty("signature", src.signature());
            return obj;
        }

        @Override
        public PlayerAppearance.Skin deserialize(com.google.gson.JsonElement json, Type typeOfT,
                                                  com.google.gson.JsonDeserializationContext context) {
            com.google.gson.JsonObject obj = json.getAsJsonObject();
            return PlayerAppearance.Skin.of(
                    obj.get("texture").getAsString(),
                    obj.has("signature") ? obj.get("signature").getAsString() : null
            );
        }
    }

    /**
     * Shuts down the persistence executor.
     */
    public void shutdown() {
        EXECUTOR.shutdown();
    }
}
