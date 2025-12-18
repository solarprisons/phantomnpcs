package prisons.solar.npclib.platform.adapter;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.platform.scheduler.Scheduler;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Adapter for platform-specific functionality.
 * Implementations handle server-specific operations.
 */
public interface PlatformAdapter {

    /**
     * Gets the name of this platform.
     *
     * @return platform name
     */
    @NotNull String name();

    /**
     * Initializes this adapter.
     *
     * @param plugin the plugin instance (platform-specific type)
     */
    void initialize(@NotNull Object plugin);

    /**
     * Shuts down this adapter.
     */
    void shutdown();

    /**
     * Gets the scheduler for this platform.
     *
     * @return the scheduler
     */
    @NotNull Scheduler scheduler();

    /**
     * Gets a viewer by UUID.
     *
     * @param uuid the player UUID
     * @return the viewer, or null if offline
     */
    Viewer viewer(@NotNull UUID uuid);

    /**
     * Gets all online viewers.
     *
     * @return collection of online viewers
     */
    @NotNull Collection<Viewer> onlineViewers();

    /**
     * Allocates a new entity ID.
     *
     * @return unique entity ID
     */
    int allocateEntityId();

    /**
     * Registers a player join listener.
     *
     * @param listener the listener
     */
    void onPlayerJoin(@NotNull Consumer<Viewer> listener);

    /**
     * Registers a player quit listener.
     *
     * @param listener the listener
     */
    void onPlayerQuit(@NotNull Consumer<Viewer> listener);

    /**
     * Registers a player world change listener.
     *
     * @param listener the listener
     */
    void onPlayerWorldChange(@NotNull Consumer<Viewer> listener);

    /**
     * Gets the world identifier from a world name.
     *
     * @param worldName the world name
     * @return the world ID
     */
    @NotNull String worldId(@NotNull String worldName);

    /**
     * Checks if a world exists.
     *
     * @param worldId the world identifier
     * @return true if exists
     */
    boolean worldExists(@NotNull String worldId);
}
