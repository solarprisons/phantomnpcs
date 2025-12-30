package prisons.solar.npclib.paper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides version and repository information for the Phantom library.
 * Values are populated at build time from the project configuration and git remote.
 */
public final class PhantomVersion {

    private static final String VERSION;
    private static final String GITHUB_OWNER;
    private static final String GITHUB_REPO;

    static {
        String loadedVersion = "unknown";
        String loadedOwner = null;
        String loadedRepo = null;

        try (InputStream is = PhantomVersion.class.getClassLoader()
                .getResourceAsStream("phantom-version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                loadedVersion = props.getProperty("version", "unknown");
                String owner = props.getProperty("github.owner", "");
                String repo = props.getProperty("github.repo", "");
                if (!owner.isEmpty()) loadedOwner = owner;
                if (!repo.isEmpty()) loadedRepo = repo;
            }
        } catch (IOException ignored) {
            // Use defaults
        }

        VERSION = loadedVersion;
        GITHUB_OWNER = loadedOwner;
        GITHUB_REPO = loadedRepo;
    }

    private PhantomVersion() {}

    /**
     * Gets the current version of the Phantom library.
     *
     * @return the version string
     */
    public static @NotNull String getVersion() {
        return VERSION;
    }

    /**
     * Gets the GitHub repository owner, if available.
     *
     * @return the owner, or null if not configured
     */
    public static @Nullable String getGitHubOwner() {
        return GITHUB_OWNER;
    }

    /**
     * Gets the GitHub repository name, if available.
     *
     * @return the repo name, or null if not configured
     */
    public static @Nullable String getGitHubRepo() {
        return GITHUB_REPO;
    }
}
