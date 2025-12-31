package prisons.solar.npclib.paper.skin;

import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import me.tofaa.entitylib.extras.skin.SkinFetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.PlayerAppearance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages skin fetching and caching for NPCs.
 */
public class SkinManager {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "Phantom-SkinFetcher");
        t.setDaemon(true);
        return t;
    });

    private static final String MINESKIN_API_URL = "https://api.mineskin.org/v2/queue";
    private static final Pattern SKINSMC_PATTERN = Pattern.compile("skinsmc\\.org/skin/(\\d+)");
    private static final Pattern TEXTURE_VALUE_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TEXTURE_SIGNATURE_PATTERN = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final SkinFetcher fetcher;
    private final Map<String, CachedSkin> nameCache = new ConcurrentHashMap<>();
    private final Map<UUID, CachedSkin> uuidCache = new ConcurrentHashMap<>();
    private final Map<String, CachedSkin> urlCache = new ConcurrentHashMap<>();

    private static final long CACHE_DURATION_MS = 1000 * 60 * 30; // 30 minutes

    private String mineSkinApiKey;
    private Logger logger;

    public SkinManager() {
        this.fetcher = SkinFetcher.builder().build();
    }

    /**
     * Sets the MineSkin API key for URL-based skin fetching.
     *
     * @param apiKey the MineSkin API key
     */
    public void setMineSkinApiKey(@NotNull String apiKey) {
        this.mineSkinApiKey = apiKey;
    }

    /**
     * Sets the logger for debug output.
     *
     * @param logger the logger
     */
    public void setLogger(@Nullable Logger logger) {
        this.logger = logger;
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
     * Fetches a skin from a URL using the MineSkin API.
     * Supports direct PNG URLs and skinsmc.org URLs.
     *
     * @param url the skin URL
     * @return future containing the skin, or null if failed
     */
    public CompletableFuture<PlayerAppearance.Skin> fetchByURL(@NotNull String url) {
        if (mineSkinApiKey == null || mineSkinApiKey.isEmpty()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("MineSkin API key not configured"));
        }

        String normalizedUrl = normalizeUrl(url);

        CachedSkin cached = urlCache.get(normalizedUrl);
        if (cached != null && !cached.isExpired()) {
            return CompletableFuture.completedFuture(cached.skin);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchFromMineSkin(normalizedUrl);
            } catch (Exception e) {
                if (logger != null) {
                    logger.warning("Failed to fetch skin from URL: " + e.getMessage());
                }
                return null;
            }
        }, EXECUTOR);
    }

    /**
     * Normalizes skin URLs to their direct PNG download URL.
     */
    private String normalizeUrl(String url) {
        Matcher skinsmc = SKINSMC_PATTERN.matcher(url);
        if (skinsmc.find()) {
            return "https://skinsmc.org/download/" + skinsmc.group(1);
        }
        return url;
    }

    /**
     * Fetches skin from MineSkin API.
     */
    private PlayerAppearance.Skin fetchFromMineSkin(String skinUrl) throws Exception {
        String jsonBody = "{\"url\":\"" + skinUrl + "\",\"variant\":\"classic\",\"visibility\":\"unlisted\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MINESKIN_API_URL))
                .header("Authorization", "Bearer " + mineSkinApiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("MineSkin API error: " + response.statusCode() + " - " + response.body());
        }

        String body = response.body();

        // Check if it's a queued job
        if (body.contains("\"job\"")) {
            return handleQueuedJob(body);
        }

        return extractSkinFromResponse(body);
    }

    /**
     * Handles queued MineSkin jobs by polling for completion.
     */
    private PlayerAppearance.Skin handleQueuedJob(String initialResponse) throws Exception {
        // Extract job ID
        Pattern jobPattern = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
        Matcher jobMatcher = jobPattern.matcher(initialResponse);
        if (!jobMatcher.find()) {
            throw new RuntimeException("Failed to extract job ID from MineSkin response");
        }
        String jobId = jobMatcher.group(1);

        // Poll for completion (max 60 seconds)
        for (int i = 0; i < 30; i++) {
            Thread.sleep(2000);

            HttpRequest pollRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mineskin.org/v2/queue/" + jobId))
                    .header("Authorization", "Bearer " + mineSkinApiKey)
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> pollResponse = HTTP_CLIENT.send(pollRequest, HttpResponse.BodyHandlers.ofString());
            String pollBody = pollResponse.body();

            if (pollBody.contains("\"completed\"") || pollBody.contains("\"texture\"")) {
                return extractSkinFromResponse(pollBody);
            }

            if (pollBody.contains("\"error\"") || pollBody.contains("\"failed\"")) {
                throw new RuntimeException("MineSkin job failed: " + pollBody);
            }
        }

        throw new RuntimeException("MineSkin job timed out after 60 seconds");
    }

    /**
     * Extracts skin texture data from MineSkin API response.
     */
    private PlayerAppearance.Skin extractSkinFromResponse(String response) {
        Matcher valueMatcher = TEXTURE_VALUE_PATTERN.matcher(response);
        Matcher signatureMatcher = TEXTURE_SIGNATURE_PATTERN.matcher(response);

        if (!valueMatcher.find()) {
            throw new RuntimeException("Failed to extract texture value from response");
        }

        String textureValue = valueMatcher.group(1);
        String signature = signatureMatcher.find() ? signatureMatcher.group(1) : null;

        PlayerAppearance.Skin skin = PlayerAppearance.Skin.of(textureValue, signature);
        urlCache.put(textureValue, new CachedSkin(skin));

        return skin;
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
        urlCache.clear();
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
