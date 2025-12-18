package prisons.solar.npclib.paper.skin;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import me.tofaa.entitylib.extras.skin.SkinFetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.PlayerAppearance;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages skin fetching and caching for NPCs.
 */
public class SkinManager {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "Phantom-SkinFetcher");
        t.setDaemon(true);
        return t;
    });

    private final SkinFetcher fetcher;
    private final Map<String, CachedSkin> nameCache = new ConcurrentHashMap<>();
    private final Map<UUID, CachedSkin> uuidCache = new ConcurrentHashMap<>();

    private static final long CACHE_DURATION_MS = 1000 * 60 * 30; // 30 minutes

    public SkinManager() {
        this.fetcher = SkinFetcher.builder().build();
    }

    /**
     * Fetches a skin by player name asynchronously.
     *
     * @param name the player name
     * @return future containing the skin, or null if not found
     */
    public CompletableFuture<PlayerAppearance.Skin> fetchByName(@NotNull String name) {
        CachedSkin cached = nameCache.get(name.toLowerCase());
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.skin);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextureProperty> properties = fetcher.getSkin(name);
                if (properties == null || properties.isEmpty()) {
                    return null;
                }

                TextureProperty prop = properties.get(0);
                PlayerAppearance.Skin skin = PlayerAppearance.Skin.of(prop.getValue(), prop.getSignature());
                nameCache.put(name.toLowerCase(), new CachedSkin(skin));
                return skin;
            } catch (Exception e) {
                return null;
            }
        }, EXECUTOR);
    }

    /**
     * Fetches a skin by player UUID asynchronously.
     *
     * @param uuid the player UUID
     * @return future containing the skin, or null if not found
     */
    public CompletableFuture<PlayerAppearance.Skin> fetchByUUID(@NotNull UUID uuid) {
        CachedSkin cached = uuidCache.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.skin);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<TextureProperty> properties = fetcher.getSkin(uuid);
                if (properties == null || properties.isEmpty()) {
                    return null;
                }

                TextureProperty prop = properties.get(0);
                PlayerAppearance.Skin skin = PlayerAppearance.Skin.of(prop.getValue(), prop.getSignature());
                uuidCache.put(uuid, new CachedSkin(skin));
                return skin;
            } catch (Exception e) {
                return null;
            }
        }, EXECUTOR);
    }

    /**
     * Converts a Phantom skin to PacketEvents TextureProperty list.
     *
     * @param skin the skin
     * @return texture property list
     */
    public static List<TextureProperty> toTextureProperties(@Nullable PlayerAppearance.Skin skin) {
        if (skin == null) {
            return List.of();
        }
        return List.of(new TextureProperty("textures", skin.texture(), skin.signature()));
    }

    /**
     * Clears the skin cache.
     */
    public void clearCache() {
        nameCache.clear();
        uuidCache.clear();
    }

    /**
     * Shuts down the skin fetcher executor.
     */
    public void shutdown() {
        EXECUTOR.shutdown();
    }

    private record CachedSkin(PlayerAppearance.Skin skin, long fetchedAt) {
        CachedSkin(PlayerAppearance.Skin skin) {
            this(skin, System.currentTimeMillis());
        }

        boolean isExpired() {
            return System.currentTimeMillis() - fetchedAt > CACHE_DURATION_MS;
        }
    }
}
