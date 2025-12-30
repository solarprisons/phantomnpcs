package prisons.solar.npclib.paper.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks for updates via GitHub Releases API.
 */
public final class UpdateChecker {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/%s/%s/releases/latest";
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(?:-(.+))?");
    private static final int TIMEOUT_MS = 5000;

    private final Plugin plugin;
    private final String owner;
    private final String repository;
    private final String currentVersion;

    /**
     * Creates a new update checker.
     *
     * @param plugin         the plugin instance for logging
     * @param owner          the GitHub repository owner
     * @param repository     the GitHub repository name
     * @param currentVersion the current version of the library
     */
    public UpdateChecker(@NotNull Plugin plugin, @NotNull String owner, @NotNull String repository, @NotNull String currentVersion) {
        this.plugin = plugin;
        this.owner = owner;
        this.repository = repository;
        this.currentVersion = currentVersion;
    }

    /**
     * Checks for updates asynchronously.
     *
     * @return a future that completes with the update result
     */
    public CompletableFuture<UpdateResult> checkAsync() {
        return CompletableFuture.supplyAsync(this::check);
    }

    /**
     * Checks for updates synchronously.
     *
     * @return the update result
     */
    public UpdateResult check() {
        try {
            String apiUrl = String.format(GITHUB_API_URL, owner, repository);
            URI uri = URI.create(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "Phantom-NPC-Library");
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);

            int responseCode = connection.getResponseCode();
            if (responseCode == 404) {
                return new UpdateResult(UpdateStatus.NO_RELEASES, currentVersion, null, null);
            }

            if (responseCode != 200) {
                return new UpdateResult(UpdateStatus.ERROR, currentVersion, null,
                        "GitHub API returned status " + responseCode);
            }

            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                JsonObject release = JsonParser.parseReader(reader).getAsJsonObject();
                String tagName = release.get("tag_name").getAsString();
                String latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                String releaseUrl = release.get("html_url").getAsString();

                int comparison = compareVersions(currentVersion, latestVersion);

                if (comparison < 0) {
                    return new UpdateResult(UpdateStatus.UPDATE_AVAILABLE, currentVersion, latestVersion, releaseUrl);
                } else if (comparison > 0) {
                    return new UpdateResult(UpdateStatus.DEV_VERSION, currentVersion, latestVersion, releaseUrl);
                } else {
                    return new UpdateResult(UpdateStatus.UP_TO_DATE, currentVersion, latestVersion, releaseUrl);
                }
            }
        } catch (IOException e) {
            return new UpdateResult(UpdateStatus.ERROR, currentVersion, null, e.getMessage());
        }
    }

    /**
     * Checks for updates and logs the result to the console.
     */
    public void checkAndNotify() {
        checkAsync().thenAccept(result -> {
            switch (result.status()) {
                case UPDATE_AVAILABLE -> {
                    plugin.getLogger().warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    plugin.getLogger().warning("A new version of Phantom is available!");
                    plugin.getLogger().warning("Current: " + result.currentVersion() + " → Latest: " + result.latestVersion());
                    plugin.getLogger().warning("Download: " + result.message());
                    plugin.getLogger().warning("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
                case DEV_VERSION -> {
                    if (plugin.getLogger().isLoggable(Level.FINE)) {
                        plugin.getLogger().info("[Phantom] Running development version " + result.currentVersion() +
                                " (latest release: " + result.latestVersion() + ")");
                    }
                }
                case UP_TO_DATE -> {
                    if (plugin.getLogger().isLoggable(Level.FINE)) {
                        plugin.getLogger().info("[Phantom] You are running the latest version (" + result.currentVersion() + ")");
                    }
                }
                case NO_RELEASES -> {
                    // No releases yet, silently ignore
                }
                case ERROR -> {
                    if (plugin.getLogger().isLoggable(Level.FINE)) {
                        plugin.getLogger().warning("[Phantom] Failed to check for updates: " + result.message());
                    }
                }
            }
        });
    }

    /**
     * Compares two semantic versions.
     *
     * @param current the current version
     * @param latest  the latest version
     * @return negative if current < latest, positive if current > latest, 0 if equal
     */
    static int compareVersions(@NotNull String current, @NotNull String latest) {
        Matcher currentMatcher = VERSION_PATTERN.matcher(current);
        Matcher latestMatcher = VERSION_PATTERN.matcher(latest);

        if (!currentMatcher.find() || !latestMatcher.find()) {
            // Fall back to string comparison if pattern doesn't match
            return current.compareTo(latest);
        }

        // Compare major.minor.patch
        for (int i = 1; i <= 3; i++) {
            int currentPart = Integer.parseInt(currentMatcher.group(i));
            int latestPart = Integer.parseInt(latestMatcher.group(i));
            if (currentPart != latestPart) {
                return currentPart - latestPart;
            }
        }

        // Compare pre-release suffixes
        String currentSuffix = currentMatcher.group(4);
        String latestSuffix = latestMatcher.group(4);

        // No suffix means release version (higher than any pre-release)
        if (currentSuffix == null && latestSuffix == null) {
            return 0;
        }
        if (currentSuffix == null) {
            return 1; // Current is release, latest is pre-release
        }
        if (latestSuffix == null) {
            return -1; // Current is pre-release, latest is release
        }

        // Both have suffixes, compare them
        return compareSuffixes(currentSuffix, latestSuffix);
    }

    private static int compareSuffixes(@NotNull String current, @NotNull String latest) {
        // Order: SNAPSHOT < alpha < beta < rc < (release)
        int currentWeight = getSuffixWeight(current);
        int latestWeight = getSuffixWeight(latest);
        return currentWeight - latestWeight;
    }

    private static int getSuffixWeight(@NotNull String suffix) {
        String lower = suffix.toLowerCase();
        if (lower.contains("snapshot")) return 0;
        if (lower.contains("alpha")) return 1;
        if (lower.contains("beta")) return 2;
        if (lower.contains("rc")) return 3;
        return 4; // Unknown suffix treated as near-release
    }

    /**
     * The status of an update check.
     */
    public enum UpdateStatus {
        /** A newer version is available */
        UPDATE_AVAILABLE,
        /** The current version is up to date */
        UP_TO_DATE,
        /** The current version is newer than the latest release (dev build) */
        DEV_VERSION,
        /** No releases found on GitHub */
        NO_RELEASES,
        /** An error occurred during the check */
        ERROR
    }

    /**
     * The result of an update check.
     *
     * @param status         the status of the check
     * @param currentVersion the current version
     * @param latestVersion  the latest version (null if error or no releases)
     * @param message        the release URL or error message
     */
    public record UpdateResult(
            @NotNull UpdateStatus status,
            @NotNull String currentVersion,
            String latestVersion,
            String message
    ) {}
}
