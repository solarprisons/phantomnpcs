package prisons.solar.npclib.paper;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.Phantom;
import prisons.solar.npclib.api.config.PhantomConfig;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.event.EventBus;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.NPCRegistry;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.service.ServiceRegistry;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.api.viewer.ViewerTracker;
import prisons.solar.npclib.core.config.TomlConfigLoader;
import prisons.solar.npclib.core.engine.AsyncVisibilityCalculator;
import prisons.solar.npclib.core.engine.NPCEngine;
import prisons.solar.npclib.core.engine.PacketBatcher;
import prisons.solar.npclib.core.engine.ProximityTracker;
import prisons.solar.npclib.core.event.SimpleEventBus;
import prisons.solar.npclib.core.hologram.SimpleHologramRegistry;
import prisons.solar.npclib.core.npc.SimpleNPCRegistry;
import prisons.solar.npclib.core.service.SimpleServiceRegistry;
import prisons.solar.npclib.core.spatial.SpatialGrid;
import prisons.solar.npclib.paper.listener.InteractionListener;
import prisons.solar.npclib.paper.npc.EntityLibArmorStandNPC;
import prisons.solar.npclib.paper.npc.EntityLibBlockDisplayNPC;
import prisons.solar.npclib.paper.npc.EntityLibItemDisplayNPC;
import prisons.solar.npclib.paper.npc.EntityLibLivingNPC;
import prisons.solar.npclib.paper.npc.EntityLibPlayerNPC;
import prisons.solar.npclib.paper.npc.EntityLibTextDisplayNPC;
import prisons.solar.npclib.paper.skin.SkinManager;
import prisons.solar.npclib.api.world.WorldProvider;
import prisons.solar.npclib.paper.world.PaperWorldProvider;
import prisons.solar.npclib.core.physics.PhysicsEngine;
import prisons.solar.npclib.core.physics.CollisionManager;
import prisons.solar.npclib.core.ai.pathfinding.PathfindingService;
import prisons.solar.npclib.api.ai.pathfinding.PathfindingComponent;
import prisons.solar.npclib.paper.update.UpdateChecker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Paper implementation of {@link Phantom}.
 * Main entry point for using Phantom NPCs on Paper servers.
 */
public class PaperPhantom implements Phantom {

    private final Plugin plugin;
    private final PaperPlatformAdapter platform;
    private final SimpleServiceRegistry services;
    private final SimpleEventBus eventBus;
    private final SimpleNPCRegistry npcRegistry;
    private final PaperViewerTracker viewerTracker;
    private final SkinManager skinManager;
    private final InteractionListener interactionListener;
    private final NPCEngine npcEngine;
    private final ProximityTracker proximityTracker;
    private final SpatialGrid spatialGrid;
    private final SimpleHologramRegistry hologramRegistry;
    private final PaperWorldProvider worldProvider;
    private final CollisionManager collisionManager;
    private final PhysicsEngine physicsEngine;
    private final ScheduledExecutorService physicsScheduler;
    private final PathfindingService pathfindingService;

    private final PhantomConfig config;
    private PacketBatcher<Object, Viewer> packetBatcher;
    private AsyncVisibilityCalculator asyncVisibilityCalculator;

    private boolean enabled = false;

    private PaperPhantom(@NotNull Plugin plugin, @NotNull PhantomConfig config) {
        this.plugin = plugin;
        this.config = config;

        // Initialize components
        this.platform = PaperPlatformAdapter.create(plugin);
        this.services = new SimpleServiceRegistry();
        this.eventBus = new SimpleEventBus();
        this.skinManager = new SkinManager();
        this.npcRegistry = new SimpleNPCRegistry(platform, this::createNPC);
        this.viewerTracker = new PaperViewerTracker(platform, npcRegistry, config.viewDistance());
        this.interactionListener = new InteractionListener(plugin, npcRegistry, platform, eventBus);
        this.npcEngine = new NPCEngine(npcRegistry);
        this.proximityTracker = new ProximityTracker();
        this.spatialGrid = new SpatialGrid(config.viewDistance());
        this.hologramRegistry = new SimpleHologramRegistry();

        // Initialize physics components
        this.worldProvider = new PaperWorldProvider();
        this.collisionManager = new CollisionManager(worldProvider);
        this.physicsScheduler = Executors.newScheduledThreadPool(1,
                r -> new Thread(r, "phantom-physics"));
        this.physicsEngine = new PhysicsEngine(collisionManager, physicsScheduler);

        // Initialize pathfinding service (HPA* with chunk-level abstraction)
        this.pathfindingService = new PathfindingService(
            worldProvider,
            4 // 4 pathfinding threads
        );

        // Register services
        services.register(SkinManager.class, skinManager);
        services.register(NPCEngine.class, npcEngine);
        services.register(ProximityTracker.class, proximityTracker);
        services.register(SpatialGrid.class, spatialGrid);
        services.register(SimpleHologramRegistry.class, hologramRegistry);
        services.register(PhantomConfig.class, config);
        services.register(CollisionManager.class, collisionManager);
        services.register(PhysicsEngine.class, physicsEngine);
        services.register(PathfindingComponent.class, pathfindingService.getPathfinder());
        services.register(WorldProvider.class, worldProvider);
    }

    private NPC<?> createNPC(EntityType type, Position position) {
        return switch (type) {
            case PLAYER -> {
                EntityLibPlayerNPC npc = new EntityLibPlayerNPC(position, services);
                npc.setSkinManager(skinManager);
                npc.setEventBus(eventBus);
                yield npc;
            }
            case ZOMBIE, SKELETON, CREEPER, VILLAGER, IRON_GOLEM, SNOW_GOLEM,
                 WITCH, PILLAGER, VINDICATOR, EVOKER, ENDERMAN, PIGLIN,
                 PIGLIN_BRUTE, ZOMBIFIED_PIGLIN, BLAZE, WITHER_SKELETON,
                 GUARDIAN, ELDER_GUARDIAN, SHULKER, WARDEN, ALLAY, VEX,
                 COW, PIG, SHEEP, CHICKEN, WOLF, CAT, HORSE, DONKEY,
                 MULE, LLAMA, FOX, PANDA, BEE, FROG, AXOLOTL, GOAT,
                 CAMEL, SNIFFER, ARMADILLO -> {
                EntityLibLivingNPC npc = new EntityLibLivingNPC(type, position, services);
                npc.setEventBus(eventBus);
                yield npc;
            }
            case ARMOR_STAND -> {
                EntityLibArmorStandNPC npc = new EntityLibArmorStandNPC(position, services);
                npc.setEventBus(eventBus);
                yield npc;
            }
            case TEXT_DISPLAY -> {
                EntityLibTextDisplayNPC npc = new EntityLibTextDisplayNPC(position, services);
                npc.setEventBus(eventBus);
                yield npc;
            }
            case BLOCK_DISPLAY -> {
                EntityLibBlockDisplayNPC npc = new EntityLibBlockDisplayNPC(position, services);
                npc.setEventBus(eventBus);
                yield npc;
            }
            case ITEM_DISPLAY -> {
                EntityLibItemDisplayNPC npc = new EntityLibItemDisplayNPC(position, services);
                npc.setEventBus(eventBus);
                yield npc;
            }
            default -> throw new UnsupportedOperationException("Entity type not yet supported: " + type);
        };
    }

    @Override
    public @NotNull NPCRegistry npcs() {
        return npcRegistry;
    }

    @Override
    public @NotNull ViewerTracker viewers() {
        return viewerTracker;
    }

    @Override
    public @NotNull EventBus events() {
        return eventBus;
    }

    @Override
    public @NotNull ServiceRegistry services() {
        return services;
    }

    @Override
    public void enable() {
        if (enabled) {
            return;
        }
        enabled = true;

        // Initialize EntityLib
        SpigotEntityLibPlatform entityLibPlatform = new SpigotEntityLibPlatform((JavaPlugin) plugin);
        APIConfig entityLibConfig = new APIConfig(PacketEvents.getAPI())
                .usePlatformLogger();
        EntityLib.init(entityLibPlatform, entityLibConfig);

        // Initialize packet batcher if enabled
        if (config.packetBatching()) {
            packetBatcher = new PacketBatcher<>(
                    this::sendPacketBatch,
                    config.packetBatchSize(),
                    config.packetBatchFlushInterval()
            );
            packetBatcher.start();
            services.register(PacketBatcher.class, packetBatcher);
            if (config.debug()) {
                plugin.getLogger().info("[Phantom] Packet batching enabled (size=" + config.packetBatchSize() + ", interval=" + config.packetBatchFlushInterval() + "ms)");
            }
        }

        // Initialize async visibility calculator if enabled
        if (config.asyncVisibility()) {
            asyncVisibilityCalculator = new AsyncVisibilityCalculator(
                    npcRegistry,
                    config.viewDistance(),
                    (npc, viewer) -> npc.spawn(viewer),
                    (npc, viewer) -> npc.despawn(viewer)
            );
            asyncVisibilityCalculator.start();
            services.register(AsyncVisibilityCalculator.class, asyncVisibilityCalculator);
            if (config.debug()) {
                plugin.getLogger().info("[Phantom] Async visibility enabled");
            }
        }

        // Register interaction listener
        interactionListener.register();

        // Register block change listener for cache invalidation
        Bukkit.getPluginManager().registerEvents(
                new prisons.solar.npclib.paper.listener.BlockChangeListener(collisionManager),
                plugin
        );

        // Register world unload listener for cleanup
        Bukkit.getPluginManager().registerEvents(
                new prisons.solar.npclib.paper.listener.WorldUnloadListener(npcRegistry, collisionManager),
                plugin
        );

        // Register player listeners
        platform.onPlayerJoin(viewer -> {
            if (config.asyncVisibility() && asyncVisibilityCalculator != null) {
                asyncVisibilityCalculator.submitViewer(viewer);
            } else {
                viewerTracker.updateVisibility(viewer);
            }
        });

        platform.onPlayerQuit(viewer -> {
            // Despawn all NPCs for this viewer
            for (NPC<?> npc : npcRegistry.all()) {
                npc.despawn(viewer);
            }
            // Clear manual overrides
            viewerTracker.clearManualOverrides(viewer);
            interactionListener.clearCooldowns();
            // Notify proximity tracker
            proximityTracker.onViewerDisconnect(viewer);
            // Notify async visibility calculator
            if (asyncVisibilityCalculator != null) {
                asyncVisibilityCalculator.onViewerDisconnect(viewer);
            }
            // Remove from packet batcher
            if (packetBatcher != null) {
                packetBatcher.removeViewer(viewer);
            }
        });

        platform.onPlayerWorldChange(viewer -> {
            if (config.asyncVisibility() && asyncVisibilityCalculator != null) {
                asyncVisibilityCalculator.submitViewer(viewer);
            } else {
                viewerTracker.updateVisibility(viewer);
            }
        });

        // Start visibility tick task
        platform.scheduler().runSyncRepeating(
                this::tickVisibility,
                config.visibilityTickRate() * 50L,
                config.visibilityTickRate() * 50L,
                TimeUnit.MILLISECONDS
        );

        // Start NPC engine tick task
        platform.scheduler().runSyncRepeating(
                npcEngine::tick,
                config.aiTickRate() * 50L,
                config.aiTickRate() * 50L,
                TimeUnit.MILLISECONDS
        );
        npcEngine.start();

        // Start proximity tracking tick task
        platform.scheduler().runSyncRepeating(
                () -> proximityTracker.tickAll(platform.onlineViewers()),
                config.proximityTickRate() * 50L,
                config.proximityTickRate() * 50L,
                TimeUnit.MILLISECONDS
        );

        // Start pathfinding budget reset task
        platform.scheduler().runSyncRepeating(
                pathfindingService::onTick,
                50L, // Every tick (50ms = 1 tick)
                50L,
                TimeUnit.MILLISECONDS
        );

        plugin.getLogger().info("[Phantom] Enabled with view distance " + config.viewDistance() + " blocks");

        // Check for updates
        String owner = PhantomVersion.getGitHubOwner();
        String repo = PhantomVersion.getGitHubRepo();
        if (config.updateChecker() && owner != null && repo != null) {
            new UpdateChecker(plugin, owner, repo, PhantomVersion.getVersion()).checkAndNotify();
        }
    }

    @SuppressWarnings("unchecked")
    private void sendPacketBatch(Viewer viewer, List<Object> packets) {
        Object user = platform.getPacketUser(viewer);
        if (user == null) {
            return;
        }

        for (Object packet : packets) {
            if (packet instanceof PacketWrapper<?> wrapper) {
                try {
                    PacketEvents.getAPI()
                            .getPlayerManager()
                            .sendPacket(user, wrapper);
                } catch (Exception e) {
                    if (config.debug()) {
                        plugin.getLogger().warning("[Phantom] Failed to send packet: " + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;

        // Stop NPC engine
        npcEngine.stop();

        // Stop packet batcher
        if (packetBatcher != null) {
            packetBatcher.stop();
        }

        // Stop async visibility calculator
        if (asyncVisibilityCalculator != null) {
            asyncVisibilityCalculator.stop();
        }

        // Clear proximity tracker
        proximityTracker.clear();

        // Unregister interaction listener
        interactionListener.unregister();

        // Despawn all NPCs
        for (NPC<?> npc : npcRegistry.all()) {
            npc.destroy();
        }

        // Clear holograms
        hologramRegistry.clear();

        // Clear spatial index
        spatialGrid.clear();

        // Shutdown pathfinding service
        pathfindingService.shutdown();

        // Shutdown skin manager
        skinManager.shutdown();

        platform.shutdown();

        plugin.getLogger().info("[Phantom] Disabled");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void invalidatePathfindingCache(@NotNull String worldId, int x, int y, int z) {
        Position position = new Position(worldId, x, y, z, 0, 0);
        pathfindingService.invalidateRegion(position);
    }

    private void tickVisibility() {
        if (config.asyncVisibility() && asyncVisibilityCalculator != null) {
            // Process async results and submit viewers for next calculation
            asyncVisibilityCalculator.processAllResults();
            asyncVisibilityCalculator.submitViewers(platform.onlineViewers());
        } else {
            // Sync visibility
            for (Viewer viewer : platform.onlineViewers()) {
                viewerTracker.updateVisibility(viewer);
            }
        }
    }

    /**
     * Gets the configuration.
     *
     * @return the config
     */
    public @NotNull PhantomConfig getConfig() {
        return config;
    }

    /**
     * Gets the packet batcher (if enabled).
     *
     * @return the packet batcher, or null if disabled
     */
    public @Nullable PacketBatcher<Object, Viewer> getPacketBatcher() {
        return packetBatcher;
    }

    /**
     * Gets the async visibility calculator (if enabled).
     *
     * @return the async visibility calculator, or null if disabled
     */
    public @Nullable AsyncVisibilityCalculator getAsyncVisibilityCalculator() {
        return asyncVisibilityCalculator;
    }

    /**
     * Gets the plugin instance.
     *
     * @return the plugin
     */
    public @NotNull Plugin getPlugin() {
        return plugin;
    }

    /**
     * Gets the platform adapter.
     *
     * @return the platform adapter
     */
    public @NotNull PaperPlatformAdapter getPlatform() {
        return platform;
    }

    /**
     * Gets the skin manager.
     *
     * @return the skin manager
     */
    public @NotNull SkinManager getSkinManager() {
        return skinManager;
    }

    /**
     * Creates a new builder for Paper Phantom.
     *
     * @param plugin the plugin
     * @return new builder
     */
    public static Builder builder(@NotNull Plugin plugin) {
        return new Builder(plugin);
    }

    /**
     * Builder for {@link PaperPhantom}.
     */
    public static class Builder implements Phantom.Builder {

        private final Plugin plugin;
        private PhantomConfig config;
        private Path configPath;

        private Builder(@NotNull Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public @NotNull Builder platform(@NotNull Object platform) {
            // Platform is already set via constructor
            return this;
        }

        @Override
        public @NotNull Builder protocol(@NotNull Object protocol) {
            // Protocol is handled by EntityLib
            return this;
        }

        @Override
        public @NotNull Builder viewDistance(double distance) {
            ensureConfigBuilder().viewDistance(distance);
            return this;
        }

        @Override
        public @NotNull Builder visibilityTickRate(int ticks) {
            ensureConfigBuilder().visibilityTickRate(ticks);
            return this;
        }

        /**
         * Sets the configuration directly.
         *
         * @param config the configuration
         * @return this builder
         */
        public @NotNull Builder config(@NotNull PhantomConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Sets the path to load configuration from.
         *
         * @param path the path to the config file
         * @return this builder
         */
        public @NotNull Builder configPath(@NotNull Path path) {
            this.configPath = path;
            return this;
        }

        /**
         * Enables or disables async visibility.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public @NotNull Builder asyncVisibility(boolean enabled) {
            ensureConfigBuilder().asyncVisibility(enabled);
            return this;
        }

        /**
         * Enables or disables packet batching.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public @NotNull Builder packetBatching(boolean enabled) {
            ensureConfigBuilder().packetBatching(enabled);
            return this;
        }

        /**
         * Sets the AI tick rate.
         *
         * @param ticks the tick rate
         * @return this builder
         */
        public @NotNull Builder aiTickRate(int ticks) {
            ensureConfigBuilder().aiTickRate(ticks);
            return this;
        }

        /**
         * Sets the proximity tick rate.
         *
         * @param ticks the tick rate
         * @return this builder
         */
        public @NotNull Builder proximityTickRate(int ticks) {
            ensureConfigBuilder().proximityTickRate(ticks);
            return this;
        }

        /**
         * Enables or disables debug mode.
         *
         * @param enabled true to enable
         * @return this builder
         */
        public @NotNull Builder debug(boolean enabled) {
            ensureConfigBuilder().debug(enabled);
            return this;
        }

        /**
         * Enables or disables update checking via GitHub Releases API.
         *
         * @param enabled true to enable (default)
         * @return this builder
         */
        public @NotNull Builder updateChecker(boolean enabled) {
            ensureConfigBuilder().updateChecker(enabled);
            return this;
        }

        private PhantomConfig.Builder configBuilder;

        private PhantomConfig.Builder ensureConfigBuilder() {
            if (configBuilder == null) {
                configBuilder = PhantomConfig.builder();
            }
            return configBuilder;
        }

        @Override
        public @NotNull Phantom build() {
            PhantomConfig finalConfig = this.config;

            // Try loading from path if specified
            if (finalConfig == null && configPath != null) {
                try {
                    finalConfig = new TomlConfigLoader("/config.toml").loadPhantomConfig(configPath);
                } catch (IOException e) {
                    plugin.getLogger().warning("[Phantom] Failed to load config from " + configPath + ": " + e.getMessage());
                }
            }

            // Use builder config if available
            if (finalConfig == null && configBuilder != null) {
                finalConfig = configBuilder.build();
            }

            // Fall back to defaults
            if (finalConfig == null) {
                finalConfig = PhantomConfig.builder().build();
            }

            return new PaperPhantom(plugin, finalConfig);
        }
    }
}
