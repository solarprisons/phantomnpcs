package prisons.solar.npclib.paper;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.platform.adapter.PlatformAdapter;
import prisons.solar.npclib.platform.scheduler.Scheduler;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Paper implementation of {@link PlatformAdapter}.
 */
public class PaperPlatformAdapter implements PlatformAdapter, Listener {

    private Plugin plugin;
    private PaperScheduler scheduler;
    private final Map<UUID, PaperViewer> viewers = new ConcurrentHashMap<>();
    private final java.util.List<Consumer<Viewer>> joinListeners = new CopyOnWriteArrayList<>();
    private final java.util.List<Consumer<Viewer>> quitListeners = new CopyOnWriteArrayList<>();
    private final java.util.List<Consumer<Viewer>> worldChangeListeners = new CopyOnWriteArrayList<>();
    private final AtomicInteger entityIdCounter = new AtomicInteger(Integer.MAX_VALUE / 2);

    @Override
    public @NotNull String name() {
        return "Paper";
    }

    @Override
    public void initialize(@NotNull Object plugin) {
        if (!(plugin instanceof Plugin bukkitPlugin)) {
            throw new IllegalArgumentException("Plugin must be a Bukkit Plugin");
        }
        this.plugin = bukkitPlugin;
        this.scheduler = new PaperScheduler(bukkitPlugin);

        // Register events
        Bukkit.getPluginManager().registerEvents(this, bukkitPlugin);

        // Initialize viewers for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            viewers.put(player.getUniqueId(), new PaperViewer(player));
        }
    }

    @Override
    public void shutdown() {
        viewers.clear();
        joinListeners.clear();
        quitListeners.clear();
        worldChangeListeners.clear();
    }

    @Override
    public @NotNull Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public Viewer viewer(@NotNull UUID uuid) {
        return viewers.get(uuid);
    }

    @Override
    public @NotNull Collection<Viewer> onlineViewers() {
        return java.util.Collections.unmodifiableCollection(viewers.values());
    }

    @Override
    public int allocateEntityId() {
        return entityIdCounter.getAndDecrement();
    }

    @Override
    public void onPlayerJoin(@NotNull Consumer<Viewer> listener) {
        joinListeners.add(listener);
    }

    @Override
    public void onPlayerQuit(@NotNull Consumer<Viewer> listener) {
        quitListeners.add(listener);
    }

    @Override
    public void onPlayerWorldChange(@NotNull Consumer<Viewer> listener) {
        worldChangeListeners.add(listener);
    }

    @Override
    public @NotNull String worldId(@NotNull String worldName) {
        return worldName;
    }

    @Override
    public boolean worldExists(@NotNull String worldId) {
        return Bukkit.getWorld(worldId) != null;
    }

    /**
     * Gets a viewer for a player, creating if needed.
     *
     * @param player the player
     * @return the viewer
     */
    public @NotNull PaperViewer getOrCreateViewer(@NotNull Player player) {
        return viewers.computeIfAbsent(player.getUniqueId(), uuid -> new PaperViewer(player));
    }

    /**
     * Gets the PacketEvents user object for a viewer.
     *
     * @param viewer the viewer
     * @return the user object, or null if not found
     */
    public Object getPacketUser(@NotNull Viewer viewer) {
        if (viewer instanceof PaperViewer paperViewer) {
            Player player = paperViewer.getPlayer();
            if (player != null && player.isOnline()) {
                return player;
            }
        }
        return null;
    }

    // Event handlers

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        PaperViewer viewer = new PaperViewer(event.getPlayer());
        viewers.put(event.getPlayer().getUniqueId(), viewer);
        for (Consumer<Viewer> listener : joinListeners) {
            listener.accept(viewer);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        PaperViewer viewer = viewers.remove(event.getPlayer().getUniqueId());
        if (viewer != null) {
            for (Consumer<Viewer> listener : quitListeners) {
                listener.accept(viewer);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        PaperViewer viewer = viewers.get(event.getPlayer().getUniqueId());
        if (viewer != null) {
            for (Consumer<Viewer> listener : worldChangeListeners) {
                listener.accept(viewer);
            }
        }
    }

    /**
     * Creates a new Paper platform adapter.
     *
     * @param plugin the plugin
     * @return the adapter
     */
    public static PaperPlatformAdapter create(@NotNull Plugin plugin) {
        PaperPlatformAdapter adapter = new PaperPlatformAdapter();
        adapter.initialize(plugin);
        return adapter;
    }
}
