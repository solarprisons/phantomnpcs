package prisons.solar.phantom.test;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.Phantom;
import prisons.solar.npclib.api.ai.GoalSelector;
import prisons.solar.npclib.api.ai.pathfinding.MovementCapabilities;
import prisons.solar.npclib.api.ai.pathfinding.PathfindingComponent;
import prisons.solar.npclib.api.animation.EntityAnimation;
import prisons.solar.npclib.api.animation.NPCAnimation;
import prisons.solar.npclib.api.appearance.LivingAppearance;
import prisons.solar.npclib.api.entity.EntityCategory;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.math.Vector3d;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.NPCState;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.status.CommonStatus;
import prisons.solar.npclib.api.status.EntityStatus;
import prisons.solar.npclib.api.world.WorldProvider;
import prisons.solar.npclib.api.component.CombatantComponent;
import prisons.solar.npclib.core.ai.goals.FollowTargetGoal;
import prisons.solar.npclib.core.ai.goals.NavigateToPositionGoal;
import prisons.solar.npclib.core.component.DefaultCombatantComponent;
import prisons.solar.npclib.paper.PaperPhantom;
import prisons.solar.npclib.paper.debug.PathfindingDebugDisplay;
import prisons.solar.npclib.core.physics.PhysicsEngine;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Test plugin for Phantom NPC library.
 * Demonstrates NPC creation, interaction handling, and lifecycle management.
 */
public class PhantomTestPlugin extends JavaPlugin implements TabCompleter, Listener {

    private static final String PREFIX = "§8[§bPhantom§8] §7";
    private static final String ERROR_PREFIX = "§8[§bPhantom§8] §c";
    private static final String SUCCESS_PREFIX = "§8[§bPhantom§8] §a";

    private Phantom phantom;
    private final Map<UUID, List<NPC<?>>> playerNpcs = new HashMap<>();
    private final Map<String, SubCommand> commands = new LinkedHashMap<>();
    private final Random random = new Random();
    private final Map<UUID, List<NPC<?>>> playerArmies = new HashMap<>();
    private final Map<UUID, Integer> npcRandomActionTasks = new HashMap<>();
    private final Map<UUID, NPC<?>> selectedNPCs = new HashMap<>();
    private final Map<UUID, NavigateToPositionGoal> activeNavGoals = new HashMap<>();
    private final Map<UUID, Double> npcSpeeds = new HashMap<>();  // Speed per NPC

    @Override
    public void onEnable() {
        initializePhantom();
        registerCommands();
    }

    @Override
    public void onDisable() {
        // Cancel all random action tasks
        npcRandomActionTasks.values().forEach(Bukkit.getScheduler()::cancelTask);
        npcRandomActionTasks.clear();

        if (phantom != null) {
            phantom.disable();
        }
    }

    private void initializePhantom() {
        getDataFolder().mkdirs();
        Path configPath = getDataFolder().toPath().resolve("phantom.yml");

        phantom = PaperPhantom.builder(this)
                .configPath(configPath)
                .debug(true)
                .build();

        phantom.enable();

        getServer().getPluginManager().registerEvents(this, this);

        var cmd = getCommand("npctest");
        if (cmd != null) {
            cmd.setTabCompleter(this);
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                NavigateToPositionGoal navGoal = activeNavGoals.get(player.getUniqueId());
                NPC<?> selectedNpc = selectedNPCs.get(player.getUniqueId());
                if (navGoal != null) {
                    PathfindingDebugDisplay.updateActionbar(player, navGoal);
                    if (selectedNpc != null) {
                        PathfindingDebugDisplay.displayPathParticles(navGoal, selectedNpc.getPosition());
                    }
                }
            }
        }, 0L, 5L);

        getLogger().info("PhantomTest enabled! Use /npctest for commands.");
    }

    private void registerCommands() {
        commands.put("spawn", new SubCommand(
                "spawn [speed] [name...]", "Spawn a player NPC (default: sprint speed)",
                (p, args) -> {
                    double speed = MovementCapabilities.DEFAULT_SPRINT_SPEED;
                    String name = "NPC";

                    if (args.length > 0) {
                        // Try parsing first arg as speed
                        try {
                            speed = Double.parseDouble(args[0]);
                            // Rest is name (joined with spaces)
                            if (args.length > 1) {
                                name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                            }
                        } catch (NumberFormatException e) {
                            // First arg is part of name, use default speed
                            name = String.join(" ", args);
                        }
                    }

                    spawnNpc(p, EntityType.PLAYER, name, speed);
                }
        ));

        commands.put("mob", new SubCommand(
                "mob <type> [speed]", "Spawn a mob NPC (zombie, villager, etc.)",
                (p, args) -> {
                    if (args.length == 0) {
                        send(p, ERROR_PREFIX + "Usage: /npctest mob <type> [speed]");
                        send(p, PREFIX + "Types: " + String.join(", ", getMobTypes()));
                        return;
                    }
                    EntityType type = parseEntityType(args[0]);
                    if (type == null) {
                        send(p, ERROR_PREFIX + "Unknown type: " + args[0]);
                        return;
                    }
                    double speed = MovementCapabilities.DEFAULT_SPRINT_SPEED;
                    if (args.length > 1) {
                        try {
                            speed = Double.parseDouble(args[1]);
                        } catch (NumberFormatException e) {
                            send(p, ERROR_PREFIX + "Invalid speed: " + args[1]);
                            return;
                        }
                    }
                    spawnNpc(p, type, type.name(), speed);
                }
        ));

        commands.put("despawn", new SubCommand(
                "despawn", "Remove your test NPC",
                (p, args) -> despawnNpc(p)
        ));

        commands.put("tp", new SubCommand(
                "tp", "Teleport NPC to your location",
                (p, args) -> {
                    NPC<?> npc = getNpc(p);
                    if (npc == null) return;
                    npc.teleport(positionOf(p.getLocation()));
                    send(p, SUCCESS_PREFIX + "Teleported NPC to your location.");
                }
        ));

        commands.put("look", new SubCommand(
                "look", "Make NPC look at you",
                (p, args) -> {
                    NPC<?> npc = getNpc(p);
                    if (npc == null) return;
                    npc.lookAt(positionOf(p.getEyeLocation()));
                    send(p, SUCCESS_PREFIX + "NPC is now looking at you.");
                }
        ));

        commands.put("interact", new SubCommand(
                "interact", "Enable click interaction feedback",
                (p, args) -> {
                    NPC<?> npc = getNpc(p);
                    if (npc == null) return;
                    npc.onClick(ctx -> {
                        Player clicker = ctx.viewer().platformPlayer();
                        clicker.sendMessage(PREFIX + "You " + ctx.clickType().name().toLowerCase().replace("_", " ") + " the NPC!");
                    });
                    send(p, SUCCESS_PREFIX + "Click handler enabled. Try interacting with your NPC!");
                }
        ));

        commands.put("glow", new SubCommand(
                "glow", "Toggle NPC glow effect",
                (p, args) -> {
                    NPC<?> npc = getNpc(p);
                    if (npc == null) return;
                    boolean glowing = !npc.appearance().isGlowing();
                    npc.appearance().setGlowing(glowing);
                    send(p, SUCCESS_PREFIX + "Glow " + (glowing ? "enabled" : "disabled") + ".");
                }
        ));

        commands.put("anim", new SubCommand(
                "anim <animation>", "Play an animation",
                (p, args) -> {
                    NPC<?> npc = getNpc(p);
                    if (npc == null) return;
                    if (args.length == 0) {
                        var supported = npc.supportedAnimations();
                        send(p, PREFIX + "Available animations for §f" + npc.entityType() + " §7(§f" + supported.size() + "§7):");
                        supported.forEach(anim ->
                                send(p, "  §8- §f" + anim.name().toLowerCase())
                        );
                        return;
                    }
                    try {
                        var animation = parseAnimation(args[0].toUpperCase(), npc);
                        npc.playAnimation(animation);
                        send(p, SUCCESS_PREFIX + "Played animation: §f" + animation.name().toLowerCase());
                    } catch (IllegalArgumentException e) {
                        send(p, ERROR_PREFIX + "Invalid animation: §f" + args[0]);
                        send(p, PREFIX + "Use §f/npctest anim §7to see available animations");
                    }
                }
        ));

        commands.put("status", new SubCommand(
                "status <status>", "Play an entity status",
                (p, args) -> {
                    NPC<?> npc = getNpc(p);
                    if (npc == null) return;
                    if (args.length == 0) {
                        var supported = npc.supportedStatuses();
                        send(p, PREFIX + "Available statuses for §f" + npc.entityType() + " §7(§f" + supported.size() + "§7):");
                        supported.forEach(status ->
                                send(p, "  §8- §f" + status.name().toLowerCase())
                        );
                        return;
                    }
                    try {
                        var status = parseStatus(args[0].toUpperCase(), npc);
                        npc.playStatus(status);
                        send(p, SUCCESS_PREFIX + "Played status: §f" + status.name().toLowerCase());
                    } catch (IllegalArgumentException e) {
                        send(p, ERROR_PREFIX + "Invalid status: §f" + args[0]);
                        send(p, PREFIX + "Use §f/npctest status §7to see available statuses");
                    }
                }
        ));

        commands.put("list", new SubCommand(
                "list", "List all registered NPCs",
                (p, args) -> {
                    var all = phantom.npcs().all();
                    send(p, PREFIX + "Registered NPCs: §f" + all.size());
                    all.forEach(npc -> send(p, "  §8- §7" + npc.getId().toString().substring(0, 8) + "... §8(" + npc.entityType() + ")"));
                }
        ));

        commands.put("clear", new SubCommand(
                "clear", "Remove all NPCs",
                (p, args) -> {
                    int count = phantom.npcs().all().size();

                    // Cancel all random action tasks
                    npcRandomActionTasks.values().forEach(Bukkit.getScheduler()::cancelTask);
                    npcRandomActionTasks.clear();

                    new ArrayList<>(phantom.npcs().all()).forEach(phantom.npcs()::unregister);
                    playerNpcs.clear();
                    playerArmies.clear();
                    npcSpeeds.clear();
                    send(p, SUCCESS_PREFIX + "Cleared " + count + " NPCs.");
                }
        ));

        commands.put("st", new SubCommand(
                "st [amount] [radius]", "Stress test: Spawn NPCs evenly spaced (default 100, 50 blocks)",
                (p, args) -> {
                    int amount = args.length > 0 ? Integer.parseInt(args[0]) : 100;
                    double radius = args.length > 1 ? Double.parseDouble(args[1]) : 50.0;

                    if (amount <= 0) {
                        send(p, ERROR_PREFIX + "Amount must be greater than 0");
                        return;
                    }
                    if (radius <= 0) {
                        send(p, ERROR_PREFIX + "Radius must be greater than 0");
                        return;
                    }

                    Location center = p.getLocation();
                    send(p, PREFIX + "Spawning §f" + amount + " §7NPCs in a §f" + radius + " §7block radius...");

                    List<NPC<?>> stressNpcs = playerNpcs.computeIfAbsent(p.getUniqueId(), k -> new ArrayList<>());

                    // Calculate even spacing using Fibonacci spiral
                    List<double[]> positions = generateEvenSpacing(amount, radius);

                    // Batch spawning: 100 NPCs per 5 ticks
                    final int BATCH_SIZE = 100;
                    final int BATCH_DELAY_TICKS = 5;
                    final int[] spawned = {0};

                    for (int batch = 0; batch < amount; batch += BATCH_SIZE) {
                        final int batchStart = batch;
                        final int batchEnd = Math.min(batch + BATCH_SIZE, amount);

                        Bukkit.getScheduler().runTaskLater(this, () -> {
                            for (int i = batchStart; i < batchEnd; i++) {
                                double[] pos = positions.get(i);
                                double x = center.getX() + pos[0];
                                double y = center.getY();
                                double z = center.getZ() + pos[1];

                                // Face toward center
                                double dx = center.getX() - x;
                                double dz = center.getZ() - z;
                                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

                                Location spawnLoc = new Location(center.getWorld(), x, y, z, yaw, 0);
                                Position position = positionOf(spawnLoc);

                                NPC<?> npc = phantom.npcs().create(EntityType.PLAYER, position);
                                npc.appearance().setCustomName("§fStress-" + i);
                                npc.appearance().setCustomNameVisible(false);

                                phantom.npcs().register(npc);
                                npc.spawn();
                                npc.enablePhysics();

                                // Store default sprint speed for stress test NPCs
                                npcSpeeds.put(npc.getId(), MovementCapabilities.DEFAULT_SPRINT_SPEED);

                                stressNpcs.add(npc);
                                spawned[0]++;
                            }

                            // Progress update
                            if (spawned[0] % 100 == 0 && spawned[0] < amount) {
                                send(p, PREFIX + "Progress: §f" + spawned[0] + "§7/§f" + amount);
                            }

                            if (spawned[0] >= amount) {
                                send(p, SUCCESS_PREFIX + "Spawned §f" + amount + " §astress test NPCs");
                                send(p, PREFIX + "Use §f/npctest clear §7to remove all NPCs");
                            }
                        }, (batch / BATCH_SIZE) * BATCH_DELAY_TICKS);
                    }
                }
        ));

        commands.put("army", new SubCommand(
                "army [size]", "Spawn an army that attacks on arm swing (default 10)",
                (p, args) -> {
                    int size = args.length > 0 ? Integer.parseInt(args[0]) : 10;

                    if (size <= 0 || size > 100) {
                        send(p, ERROR_PREFIX + "Size must be between 1 and 100");
                        return;
                    }

                    Location center = p.getLocation();
                    List<NPC<?>> army = new ArrayList<>();

                    double radius = 5.0;
                    for (int i = 0; i < size; i++) {
                        double angle = (2 * Math.PI * i) / size;
                        double x = center.getX() + radius * Math.cos(angle);
                        double z = center.getZ() + radius * Math.sin(angle);

                        Location loc = new Location(center.getWorld(), x, center.getY(), z);
                        Position pos = positionOf(loc);

                        NPC<?> npc = phantom.npcs().create(EntityType.PLAYER, pos);
                        npc.appearance().setCustomName("§cSoldier-" + i);
                        npc.appearance().setCustomNameVisible(true);

                        phantom.npcs().register(npc);
                        npc.spawn();
                        npc.enablePhysics();

                        // Store default sprint speed for army NPCs
                        npcSpeeds.put(npc.getId(), MovementCapabilities.DEFAULT_SPRINT_SPEED);

                        army.add(npc);
                    }

                    playerArmies.put(p.getUniqueId(), army);
                    send(p, SUCCESS_PREFIX + "Spawned army of §f" + size + " §asoldiers");
                    send(p, PREFIX + "Swing your arm to make them attack!");
                }
        ));

        commands.put("perf", new SubCommand(
                "perf <start|stop|report|reset>", "Performance profiling commands",
                (p, args) -> {
                    if (args.length == 0) {
                        send(p, PREFIX + "Performance Profiling Commands:");
                        send(p, "  §e/npctest perf start §8- §7Start profiling");
                        send(p, "  §e/npctest perf stop §8- §7Stop and show summary");
                        send(p, "  §e/npctest perf report §8- §7Show current metrics");
                        send(p, "  §e/npctest perf reset §8- §7Reset all metrics");
                        return;
                    }

                    PhysicsEngine physicsEngine = phantom.services().get(PhysicsEngine.class).orElse(null);
                    if (physicsEngine == null) {
                        send(p, ERROR_PREFIX + "PhysicsEngine not available");
                        return;
                    }

                    String subcommand = args[0].toLowerCase();
                    switch (subcommand) {
                        case "start" -> {
                            physicsEngine.resetMetrics();
                            send(p, SUCCESS_PREFIX + "Started performance profiling");
                            send(p, PREFIX + "Metrics reset. Use §f/npctest perf stop §7to see results");
                        }
                        case "stop", "report" -> {
                            send(p, "§6§l=== NPC Physics Performance Report ===");
                            send(p, "");

                            // Basic stats
                            int totalNPCs = phantom.npcs().all().size();
                            int physicsNPCs = physicsEngine.getEntities().size();
                            send(p, "§eTotal NPCs: §f" + totalNPCs);
                            send(p, "§ePhysics-enabled: §f" + physicsNPCs);
                            send(p, "");

                            // Tick performance
                            long totalTicks = physicsEngine.getTotalTicks();
                            double avgTickTime = physicsEngine.getAverageTickTime();
                            double slowestTick = physicsEngine.getSlowestTickTime();
                            double lastTick = physicsEngine.getLastTickTime();

                            send(p, "§6Tick Performance:");
                            send(p, "  §eTotal ticks: §f" + totalTicks);
                            send(p, "  §eAverage: " + formatTickTime(avgTickTime));
                            send(p, "  §eSlowest: " + formatTickTime(slowestTick));
                            send(p, "  §eLast: " + formatTickTime(lastTick));
                            send(p, "");

                            // Optimization metrics
                            long skippedNoViewers = physicsEngine.getEntitiesSkippedNoViewers();
                            long skippedThreshold = physicsEngine.getEntitiesSkippedMovementThreshold();
                            long totalSkipped = skippedNoViewers + skippedThreshold;
                            long totalProcessed = totalTicks * physicsNPCs;
                            double skipRate = totalProcessed > 0 ? (totalSkipped * 100.0 / totalProcessed) : 0;

                            send(p, "§6Optimization Stats:");
                            send(p, "  §eSkipped (no viewers): §f" + skippedNoViewers);
                            send(p, "  §eSkipped (threshold): §f" + skippedThreshold);
                            send(p, "  §eSkip rate: §f" + String.format("%.1f%%", skipRate));
                            send(p, "");

                            // Pool statistics
                            var poolStats = physicsEngine.getPoolStatistics();
                            send(p, "§6Object Pool Stats:");
                            send(p, "  §eVector3d borrows: §f" + poolStats.vector3dPool().totalBorrows());
                            send(p, "  §eVector3d exhaustions: §f" + poolStats.vector3dPool().exhaustions());
                            send(p, "  §eVector3d hit rate: §f" + String.format("%.1f%%", poolStats.vector3dPool().hitRate() * 100));
                            send(p, "  §eAABB borrows: §f" + poolStats.aabbPool().totalBorrows());
                            send(p, "  §eAABB exhaustions: §f" + poolStats.aabbPool().exhaustions());
                            send(p, "  §eAABB hit rate: §f" + String.format("%.1f%%", poolStats.aabbPool().hitRate() * 100));
                        }
                        case "reset" -> {
                            physicsEngine.resetMetrics();
                            send(p, SUCCESS_PREFIX + "Reset all performance metrics");
                        }
                        default -> send(p, ERROR_PREFIX + "Unknown subcommand: " + subcommand);
                    }
                }
        ));

        commands.put("viewdist", new SubCommand(
                "viewdist [distance]", "Get or set view distance in real-time",
                (p, args) -> {
                    var visibilityCalc = phantom.services().get(prisons.solar.npclib.core.engine.AsyncVisibilityCalculator.class);

                    if (visibilityCalc.isEmpty()) {
                        send(p, ERROR_PREFIX + "AsyncVisibilityCalculator not available");
                        return;
                    }

                    var calc = visibilityCalc.get();

                    if (args.length == 0) {
                        // Show current view distance
                        double viewDistance = calc.getViewDistance();
                        send(p, "§6§lView Distance Settings");
                        send(p, "  §eCurrent view distance: §f" + String.format("%.1f", viewDistance) + " blocks");
                        send(p, "  §eView distance squared: §f" + String.format("%.1f", viewDistance * viewDistance));
                        send(p, "");
                        send(p, "§7Use §f/npctest viewdist <distance> §7to change it");
                        return;
                    }

                    // Set new view distance
                    try {
                        double distance = Double.parseDouble(args[0]);
                        if (distance < 1 || distance > 512) {
                            send(p, ERROR_PREFIX + "Distance must be between 1 and 512 blocks");
                            return;
                        }

                        // Update view distance
                        calc.setViewDistance(distance);

                        int npcCount = phantom.npcs().all().size();
                        send(p, SUCCESS_PREFIX + "View distance updated to §f" + String.format("%.1f", distance) + " §ablocks");
                        send(p, PREFIX + "Visibility recalculating for §f" + npcCount + " §7NPCs...");
                        send(p, PREFIX + "NPCs will show/hide automatically!");
                    } catch (NumberFormatException e) {
                        send(p, ERROR_PREFIX + "Invalid number: §f" + args[0]);
                    }
                }
        ));

        commands.put("select", new SubCommand(
                "select", "Select an NPC by looking at it (within 512 blocks)",
                (p, args) -> {
                    Location eyeLoc = p.getEyeLocation();
                    Vector direction = eyeLoc.getDirection();

                    NPC<?> closestNPC = null;
                    double closestDist = Double.MAX_VALUE;

                    // Check all NPCs within 512 blocks
                    for (NPC<?> npc : phantom.npcs().all()) {
                        Position npcPos = npc.getPosition();
                        if (!npcPos.worldId().equals(p.getWorld().getUID().toString())) continue;

                        // Vector from eye to NPC
                        Vector toNPC = new Vector(
                            npcPos.x() - eyeLoc.getX(),
                            npcPos.y() - eyeLoc.getY(),
                            npcPos.z() - eyeLoc.getZ()
                        );

                        double dist = toNPC.length();
                        if (dist > 512) continue;

                        // Check if within look direction cone (15 degrees)
                        double angle = Math.toDegrees(toNPC.angle(direction));
                        if (angle < 15 && dist < closestDist) {
                            closestNPC = npc;
                            closestDist = dist;
                        }
                    }

                    if (closestNPC != null) {
                        selectedNPCs.put(p.getUniqueId(), closestNPC);
                        send(p, SUCCESS_PREFIX + "Selected NPC: §f" + closestNPC.getId() +
                                  " §a(" + String.format("%.1f", closestDist) + " blocks away)");
                    } else {
                        send(p, ERROR_PREFIX + "No NPC in line of sight (within 512 blocks)");
                    }
                }
        ));

        commands.put("follow", new SubCommand(
                "follow [player (optional)]",
                "make npc follow a target",
                (p, args) -> {
                    NPC<?> selectedNPC = selectedNPCs.get(p.getUniqueId());
                    if (selectedNPC == null) {
                        send(p, ERROR_PREFIX + "No NPC selected. Use §f/npctest select §cfirst");
                        return;
                    }

                    String playerName = args.length > 0 ? args[0].toLowerCase() : p.getName();
                    Player other = Bukkit.getPlayer(playerName);
                    if (other == null) {
                        send(p, ERROR_PREFIX + "Player " + args[0] + " not found.");
                        return;
                    }
                    GoalSelector selector = selectedNPC.getGoalSelector();

                    // Get services
                    var pathfinder = phantom.services().get(PathfindingComponent.class).orElse(null);
                    var physicsEngine = phantom.services().get(PhysicsEngine.class).orElse(null);
                    var worldProvider = phantom.services().get(WorldProvider.class).orElse(null);

                    if (pathfinder == null || physicsEngine == null || worldProvider == null) {
                        send(p, ERROR_PREFIX + "Pathfinding, Physics, or WorldProvider service not available");
                        return;
                    }

                    Position targetPos = Position.of(
                            p.getWorld().getUID().toString(),
                            p.getX() + 0.5,
                            p.getY() + 1.0, // Stand on top of block
                            p.getZ() + 0.5,
                            0, 0
                    );

                    // Use stored speed for this NPC, default to sprint speed
                    double speed = npcSpeeds.getOrDefault(selectedNPC.getId(), MovementCapabilities.DEFAULT_SPRINT_SPEED);

                    NavigateToPositionGoal navGoal = new NavigateToPositionGoal(
                            targetPos,
                            speed,
                            getMovementCapabilities(selectedNPC),
                            pathfinder,
                            worldProvider
                    );

                    Supplier<Position> posSup = () -> {
                        if (other.isDead() || !other.isOnline()){
                            return null;
                        }
                        Location loc = other.getLocation();
                        return Position.of(
                                loc.getWorld().getUID().toString(),
                                loc.getX(),
                                loc.getY(),
                                loc.getZ()
                        );
                    };

                    double followSpeed = npcSpeeds.getOrDefault(selectedNPC.getId(), MovementCapabilities.DEFAULT_SPRINT_SPEED);
                    FollowTargetGoal goal = new FollowTargetGoal(
                            pathfinder,
                            worldProvider,
                            posSup,
                            1.5,
                            3.0,
                            128.0,
                            getMovementCapabilities(selectedNPC),
                            followSpeed,
                            true  // mimicVerticalMovement - Cosmic-style guards
                    );

                    if (selector == null) {
                        send(p, ERROR_PREFIX + "Selected NPC has no goal selector");
                        return;
                    }
                    selector.addGoal(goal);
                }
        ));

        commands.put("path", new SubCommand(
                "path [me|block|raycast]", "Make selected NPC navigate to target",
                (p, args) -> {
                    NPC<?> selectedNPC = selectedNPCs.get(p.getUniqueId());
                    if (selectedNPC == null) {
                        send(p, ERROR_PREFIX + "No NPC selected. Use §f/npctest select §cfirst");
                        return;
                    }

                    // Determine target mode
                    String mode = args.length > 0 ? args[0].toLowerCase() : "raycast";
                    Block targetBlock;
                    String modeDescription;

                    if (mode.equals("me")) {
                        // Use block player is standing on
                        targetBlock = p.getLocation().subtract(0, 1, 0).getBlock();
                        modeDescription = "your position";
                    } else if (mode.equals("block") || mode.equals("raycast")) {
                        // Raycast to find block player is looking at
                        // Use fluids=false to ignore water/lava, this helps detect blocks better
                        targetBlock = p.getTargetBlockExact(512, org.bukkit.FluidCollisionMode.NEVER);
                        if (targetBlock == null || targetBlock.getType() == Material.AIR) {
                            send(p, ERROR_PREFIX + "No block in line of sight (within 512 blocks)");
                            send(p, PREFIX + "Hint: Try §f/npctest path me §7to navigate to your position");
                            return;
                        }
                        modeDescription = "block you're looking at";
                    } else {
                        send(p, ERROR_PREFIX + "Invalid mode. Use: §fme§c, §fblock§c, or §fraycast");
                        return;
                    }

                    // Get services
                    var pathfinder = phantom.services().get(PathfindingComponent.class).orElse(null);
                    var physicsEngine = phantom.services().get(PhysicsEngine.class).orElse(null);
                    var worldProvider = phantom.services().get(WorldProvider.class).orElse(null);

                    if (pathfinder == null || physicsEngine == null || worldProvider == null) {
                        send(p, ERROR_PREFIX + "Pathfinding, Physics, or WorldProvider service not available");
                        return;
                    }

                    // Create navigation goal
                    Location targetLoc = targetBlock.getLocation();
                    Position targetPos = Position.of(
                        p.getWorld().getUID().toString(),
                        targetLoc.getX() + 0.5,
                        targetLoc.getY() + 1.0, // Stand on top of block
                        targetLoc.getZ() + 0.5,
                        0, 0
                    );

                    // Use stored speed for this NPC, default to sprint speed
                    double speed = npcSpeeds.getOrDefault(selectedNPC.getId(), MovementCapabilities.DEFAULT_SPRINT_SPEED);

                    NavigateToPositionGoal navGoal = new NavigateToPositionGoal(
                        targetPos,
                        speed,
                        getMovementCapabilities(selectedNPC),
                        pathfinder,
                        worldProvider
                    );

                    // Add goal to NPC's GoalSelector
                    GoalSelector goalSelector = selectedNPC.getGoalSelector();
                    if (goalSelector != null) {
                        // Remove any existing navigation goals before adding new one
                        goalSelector.removeGoals(NavigateToPositionGoal.class);

                        goalSelector.addGoal(navGoal);
                        activeNavGoals.put(p.getUniqueId(), navGoal);
                        send(p, SUCCESS_PREFIX + "NPC navigating to §f" +
                                  targetBlock.getType() + " §aat §f" +
                                  targetLoc.getBlockX() + ", " + targetLoc.getBlockY() + ", " + targetLoc.getBlockZ());
                    } else {
                        send(p, ERROR_PREFIX + "Selected NPC has no goal selector");
                    }
                }
        ));

        commands.put("stpath", new SubCommand(
                "stpath <distance>", "Make all NPCs pathfind to random positions within distance",
                (p, args) -> {
                    if (args.length == 0) {
                        send(p, ERROR_PREFIX + "Usage: /npctest stpath <distance>");
                        send(p, PREFIX + "Example: /npctest stpath 50");
                        return;
                    }

                    double distance;
                    try {
                        distance = Double.parseDouble(args[0]);
                        if (distance <= 0 || distance > 512) {
                            send(p, ERROR_PREFIX + "Distance must be between 1 and 512 blocks");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        send(p, ERROR_PREFIX + "Invalid distance: §f" + args[0]);
                        return;
                    }

                    // Get services
                    var pathfinder = phantom.services().get(PathfindingComponent.class).orElse(null);
                    var physicsEngine = phantom.services().get(PhysicsEngine.class).orElse(null);
                    var worldProvider = phantom.services().get(WorldProvider.class).orElse(null);

                    if (pathfinder == null || physicsEngine == null || worldProvider == null) {
                        send(p, ERROR_PREFIX + "Pathfinding, Physics, or WorldProvider service not available");
                        return;
                    }

                    // Get all spawned NPCs
                    List<NPC<?>> allNPCs = new ArrayList<>(phantom.npcs().all());
                    int npcCount = allNPCs.size();

                    if (npcCount == 0) {
                        send(p, ERROR_PREFIX + "No NPCs spawned. Use §f/npctest st §cfirst");
                        return;
                    }

                    send(p, PREFIX + "Creating pathfinding goals for §f" + npcCount + " §7NPCs within §f" + distance + " §7blocks...");

                    Location playerLoc = p.getLocation();
                    int goalsCreated = 0;
                    int goalsAdded = 0;

                    for (NPC<?> npc : allNPCs) {
                        double randomAngle = random.nextDouble() * 2 * Math.PI;
                        double randomRadius = Math.sqrt(random.nextDouble()) * distance;

                        double targetX = playerLoc.getX() + randomRadius * Math.cos(randomAngle);
                        double targetZ = playerLoc.getZ() + randomRadius * Math.sin(randomAngle);
                        double targetY = playerLoc.getY(); // Same Y level as player

                        Position targetPos = Position.of(
                            p.getWorld().getUID().toString(),
                            targetX,
                            targetY,
                            targetZ,
                            0, 0
                        );

                        double npcSpeed = npcSpeeds.getOrDefault(npc.getId(), MovementCapabilities.DEFAULT_SPRINT_SPEED);

                        NavigateToPositionGoal navGoal = new NavigateToPositionGoal(
                            targetPos,
                            npcSpeed,
                            getMovementCapabilities(npc),
                            pathfinder,
                            worldProvider
                        );
                        goalsCreated++;

                        GoalSelector goalSelector = npc.getGoalSelector();
                        if (goalSelector != null) {
                            goalSelector.addGoal(navGoal);
                            goalsAdded++;
                        }
                    }

                    send(p, SUCCESS_PREFIX + "Created §f" + goalsCreated + " §apathfinding goals");
                    send(p, PREFIX + "Added §f" + goalsAdded + "§7/§f" + goalsCreated + " §7goals to NPCs");
                    send(p, PREFIX + "NPCs should start pathfinding in §f~" + (phantom.services().get(prisons.solar.npclib.core.engine.NPCEngine.class).map(e -> "1").orElse("?")) + " §7tick(s)");
                }
        ));

        commands.put("component", new SubCommand(
                "component <add|remove> <types...>", "Add/remove components (combatant,physics,pathfinding)",
                (p, args) -> {
                    if (args.length < 2) {
                        send(p, ERROR_PREFIX + "Usage: /npctest component <add|remove> <type1,type2,...>");
                        send(p, PREFIX + "Available: §fcombatant§7, §fphysics");
                        send(p, PREFIX + "Example: §f/npctest component add combatant,physics");
                        return;
                    }

                    NPC<?> selectedNPC = selectedNPCs.get(p.getUniqueId());
                    if (selectedNPC == null) {
                        send(p, ERROR_PREFIX + "No NPC selected. Use §f/npctest select §cfirst");
                        return;
                    }

                    String action = args[0].toLowerCase();
                    String[] types = args[1].toLowerCase().split(",");

                    if (!action.equals("add") && !action.equals("remove")) {
                        send(p, ERROR_PREFIX + "Invalid action. Use §fadd §cor §fremove");
                        return;
                    }

                    for (String type : types) {
                        type = type.trim();
                        switch (type) {
                            case "combatant" -> {
                                if (action.equals("add")) {
                                    if (selectedNPC.hasComponent(CombatantComponent.class)) {
                                        send(p, PREFIX + "NPC already has §fcombatant §7component");
                                    } else {
                                        selectedNPC.addComponent(new DefaultCombatantComponent(20.0f));
                                        send(p, SUCCESS_PREFIX + "Added §fcombatant §acomponent (20 HP)");
                                    }
                                } else {
                                    if (selectedNPC.removeComponent(CombatantComponent.class)) {
                                        send(p, SUCCESS_PREFIX + "Removed §fcombatant §acomponent");
                                    } else {
                                        send(p, PREFIX + "NPC doesn't have §fcombatant §7component");
                                    }
                                }
                            }
                            case "physics" -> {
                                if (action.equals("add")) {
                                    if (selectedNPC.hasPhysics()) {
                                        send(p, PREFIX + "NPC already has §fphysics §7enabled");
                                    } else {
                                        selectedNPC.enablePhysics();
                                        send(p, SUCCESS_PREFIX + "Enabled §fphysics §afor NPC");
                                    }
                                } else {
                                    if (selectedNPC.hasPhysics()) {
                                        selectedNPC.disablePhysics();
                                        send(p, SUCCESS_PREFIX + "Disabled §fphysics §afor NPC");
                                    } else {
                                        send(p, PREFIX + "NPC doesn't have §fphysics §7enabled");
                                    }
                                }
                            }
                            default -> send(p, ERROR_PREFIX + "Unknown component: §f" + type);
                        }
                    }
                }
        ));
    }

    /**
     * Gets MovementCapabilities for an NPC with its stored speed.
     */
    private MovementCapabilities getMovementCapabilities(NPC<?> npc) {
        double speed = npcSpeeds.getOrDefault(npc.getId(), MovementCapabilities.DEFAULT_SPRINT_SPEED);
        return MovementCapabilities.DEFAULT.withSpeeds(speed, speed);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block b = event.getBlock();
        phantom.invalidatePathfindingCache(
                b.getWorld().getUID().toString(),
                b.getX(),
                b.getY(),
                b.getZ()
        );
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        phantom.invalidatePathfindingCache(
                b.getWorld().getUID().toString(),
                b.getX(),
                b.getY(),
                b.getZ()
        );
    }

    @EventHandler
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        List<NPC<?>> army = playerArmies.get(player.getUniqueId());
        if (army == null || army.isEmpty()) return;

        // Make all army NPCs play attack animation
        for (NPC<?> npc : army) {
            if (npc.state() != NPCState.SPAWNED) continue;

            var attackAnim = npc.supportedAnimations().stream()
                    .filter(a -> a.name().contains("SWING") || a.name().contains("ATTACK"))
                    .findFirst();

            attackAnim.ifPresent(npc::playAnimation);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        SubCommand subCmd = commands.get(args[0].toLowerCase());
        if (subCmd == null) {
            sendHelp(player);
            return true;
        }

        String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        subCmd.execute(player, subArgs);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                                 @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(commands.keySet(), args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "mob" -> filter(getMobTypes(), args[1]);
                case "anim" -> {
                    if (!(sender instanceof Player player)) yield List.of();
                    List<NPC<?>> npcs = playerNpcs.get(player.getUniqueId());
                    if (npcs == null || npcs.isEmpty()) yield List.of();
                    NPC<?> npc = npcs.get(0);
                    yield filter(
                            npc.supportedAnimations().stream()
                                    .map(NPCAnimation::name)
                                    .map(String::toLowerCase)
                                    .toList(),
                            args[1]
                    );
                }
                case "status" -> {
                    if (!(sender instanceof Player player)) yield List.of();
                    List<NPC<?>> npcs = playerNpcs.get(player.getUniqueId());
                    if (npcs == null || npcs.isEmpty()) yield List.of();
                    NPC<?> npc = npcs.get(0);
                    yield filter(
                            npc.supportedStatuses().stream()
                                    .map(EntityStatus::name)
                                    .map(String::toLowerCase)
                                    .toList(),
                            args[1]
                    );
                }
                case "perf" -> filter(List.of("start", "stop", "report", "reset"), args[1]);
                case "stpath" -> filter(List.of("25", "50", "100", "200"), args[1]);
                case "path" -> filter(List.of("me", "block", "raycast"), args[1]);
                case "component" -> filter(List.of("add", "remove"), args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("component")) {
            // Support WorldEdit-style comma-separated completions
            String current = args[2];
            String prefix = "";
            int lastComma = current.lastIndexOf(',');
            if (lastComma >= 0) {
                prefix = current.substring(0, lastComma + 1);
                current = current.substring(lastComma + 1);
            }
            List<String> types = List.of("combatant", "physics");
            String finalPrefix = prefix;
            String finalCurrent = current;
            return types.stream()
                    .filter(t -> t.startsWith(finalCurrent.toLowerCase()))
                    .map(t -> finalPrefix + t)
                    .toList();
        }
        return List.of();
    }

    private void sendHelp(Player player) {
        send(player, "§6§lPhantom NPC Test Commands");
        commands.forEach((name, cmd) ->
                send(player, "  §e/npctest " + cmd.usage + " §8- §7" + cmd.description)
        );
    }

    private void spawnNpc(Player player, EntityType type, String name, double speed) {
        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        loc.setY(player.getLocation().getY());

        NPC<?> npc = phantom.npcs().create(type, positionOf(loc));
        npc.appearance().setCustomName("§f" + name);
        npc.appearance().setCustomNameVisible(true);

        phantom.npcs().register(npc);
        npc.spawn();
        npc.enablePhysics();

        // Store speed for this NPC
        npcSpeeds.put(npc.getId(), speed);

        playerNpcs.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(npc);

        // Auto-select the spawned NPC for convenience
        selectedNPCs.put(player.getUniqueId(), npc);

        send(player, SUCCESS_PREFIX + "Spawned §f" + type + " §anamed §f" + name + " §aat speed §f" + String.format("%.2f", speed) + " §a(auto-selected)");
    }

    private void despawnNpc(Player player) {
        List<NPC<?>> npcs = playerNpcs.remove(player.getUniqueId());
        if (npcs == null || npcs.isEmpty()) {
            send(player, ERROR_PREFIX + "You don't have any NPCs.");
            return;
        }
        npcs.forEach(phantom.npcs()::unregister);
        send(player, SUCCESS_PREFIX + "Removed your NPCs.");
    }

    private @Nullable NPC<?> getNpc(Player player) {
        List<NPC<?>> npcs = playerNpcs.get(player.getUniqueId());
        if (npcs == null || npcs.isEmpty()) {
            send(player, ERROR_PREFIX + "You don't have an NPC. Use /npctest spawn first.");
            return null;
        }
        return npcs.get(0);
    }

    /**
     * Generates evenly spaced positions using Fibonacci spiral.
     */
    private static List<double[]> generateEvenSpacing(int count, double radius) {
        List<double[]> positions = new ArrayList<>();
        double goldenRatio = (1 + Math.sqrt(5)) / 2;
        double angleIncrement = 2 * Math.PI * goldenRatio;

        for (int i = 0; i < count; i++) {
            double t = (double) i / count;
            double r = radius * Math.sqrt(t);
            double theta = i * angleIncrement;

            double x = r * Math.cos(theta);
            double z = r * Math.sin(theta);

            positions.add(new double[]{x, z});
        }

        return positions;
    }

    private static String formatTickTime(double ms) {
        if (ms < 1.0) return String.format("§a%.2fms", ms);
        if (ms < 5.0) return String.format("§e%.2fms", ms);
        return String.format("§c%.2fms", ms);
    }

    private static Position positionOf(Location loc) {
        return Position.of(loc.getWorld().getUID().toString(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    private static void send(Player player, String message) {
        player.sendMessage(message);
    }

    private static List<String> filter(Collection<String> options, String prefix) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();
    }

    private static List<String> getMobTypes() {
        return Stream.of(EntityType.values())
                .filter(t -> t.category() == EntityCategory.MOB ||
                             t.category() == EntityCategory.DISPLAY ||
                             t == EntityType.ARMOR_STAND)
                .map(Enum::name)
                .map(String::toLowerCase)
                .toList();
    }

    private static @Nullable EntityType parseEntityType(String name) {
        try {
            return EntityType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static NPCAnimation parseAnimation(String name, NPC<?> npc) {
        return npc.supportedAnimations().stream()
                .filter(anim -> anim.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown animation: " + name));
    }

    private static EntityStatus parseStatus(String name, NPC<?> npc) {
        try {
            return CommonStatus.valueOf(name);
        } catch (IllegalArgumentException ignored) {
        }

        return npc.supportedStatuses().stream()
                .filter(status -> status.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown status: " + name));
    }

    /**
     * Schedules random actions for an NPC to make stress testing more realistic.
     * NPCs will randomly jump, change velocity, play animations, etc.
     */
    private void scheduleRandomActions(NPC<?> npc) {
        int taskId = getServer().getScheduler().runTaskTimer(this, () -> {
            // Check if NPC is destroyed
            if (npc.state() == NPCState.DESTROYED) {
                Integer task = npcRandomActionTasks.remove(npc.getId());
                if (task != null) {
                    Bukkit.getScheduler().cancelTask(task);
                }
                return;
            }

            try {
                // Null checks to prevent NPE
                Position npcPos = npc.getPosition();
                if (npcPos == null) return;

                Vector3d currentVel = npc.getVelocity();
                if (currentVel == null) return;

                // Random actions (50% chance each tick)
                if (random.nextDouble() < 0.5) {
                    int action = random.nextInt(4);

                    switch (action) {
                        case 0 -> {
                            // Jump
                            npc.setVelocity(new Vector3d(currentVel.getX(), 8.0, currentVel.getZ()));
                        }
                        case 1 -> {
                            // Random movement
                            double vx = (random.nextDouble() - 0.5) * 4.0;
                            double vz = (random.nextDouble() - 0.5) * 4.0;
                            npc.setVelocity(new Vector3d(vx, currentVel.getY(), vz));
                        }
                        case 2 -> {
                            // Play animation
                            var anims = npc.supportedAnimations();
                            if (!anims.isEmpty()) {
                                var animList = new ArrayList<>(anims);
                                var anim = animList.get(random.nextInt(animList.size()));
                                npc.playAnimation(anim);
                            }
                        }
                        case 3 -> {
                            // Random rotation
                            float randomYaw = random.nextFloat() * 360f;
                            Position newPos = new Position(
                                    npcPos.worldId(),
                                    npcPos.x(),
                                    npcPos.y(),
                                    npcPos.z(),
                                    randomYaw,
                                    npcPos.pitch()
                            );
                            npc.teleport(newPos, true);
                        }
                    }
                }
            } catch (Exception e) {
                // Silently catch and cancel task to prevent spam
                Integer task = npcRandomActionTasks.remove(npc.getId());
                if (task != null) {
                    Bukkit.getScheduler().cancelTask(task);
                }
            }
        }, random.nextInt(10) + 10L, random.nextInt(20) + 10L).getTaskId();

        npcRandomActionTasks.put(npc.getId(), taskId);
    }

    private record SubCommand(String usage, String description, BiConsumer<Player, String[]> handler) {
        void execute(Player player, String[] args) {
            handler.accept(player, args);
        }
    }
}
