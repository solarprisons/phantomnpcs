package prisons.solar.phantom.test;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.Phantom;
import prisons.solar.npclib.api.animation.EntityAnimation;
import prisons.solar.npclib.api.animation.MobAnimation;
import prisons.solar.npclib.api.animation.NPCAnimation;
import prisons.solar.npclib.api.entity.EntityCategory;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.status.CommonStatus;
import prisons.solar.npclib.paper.PaperPhantom;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Test plugin for Phantom NPC library.
 * Demonstrates NPC creation, interaction handling, and lifecycle management.
 */
public class PhantomTestPlugin extends JavaPlugin implements TabCompleter {

    private static final String PREFIX = "§8[§bPhantom§8] §7";
    private static final String ERROR_PREFIX = "§8[§bPhantom§8] §c";
    private static final String SUCCESS_PREFIX = "§8[§bPhantom§8] §a";

    private Phantom phantom;
    private final Map<UUID, NPC<?>> playerNpcs = new HashMap<>();
    private final Map<String, SubCommand> commands = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        initializePhantom();
        registerCommands();

        // Register tab completer
        var cmd = getCommand("npctest");
        if (cmd != null) {
            cmd.setTabCompleter(this);
        }

        getLogger().info("PhantomTest enabled! Use /npctest for commands.");
    }

    @Override
    public void onDisable() {
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
    }

    private void registerCommands() {
        commands.put("spawn", new SubCommand(
                "spawn [name]", "Spawn a player NPC",
                (p, args) -> spawnNpc(p, EntityType.PLAYER, args.length > 0 ? args[0] : "NPC")
        ));

        commands.put("mob", new SubCommand(
                "mob <type>", "Spawn a mob NPC (zombie, villager, etc.)",
                (p, args) -> {
                    if (args.length == 0) {
                        send(p, ERROR_PREFIX + "Usage: /npctest mob <type>");
                        send(p, PREFIX + "Types: " + String.join(", ", getMobTypes()));
                        return;
                    }
                    EntityType type = parseEntityType(args[0]);
                    if (type == null) {
                        send(p, ERROR_PREFIX + "Unknown type: " + args[0]);
                        return;
                    }
                    spawnNpc(p, type, type.name());
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
                    all.forEach(npc -> send(p, "  §8- §7" + npc.id().toString().substring(0, 8) + "... §8(" + npc.entityType() + ")"));
                }
        ));

        commands.put("clear", new SubCommand(
                "clear", "Remove all NPCs",
                (p, args) -> {
                    int count = phantom.npcs().all().size();
                    new ArrayList<>(phantom.npcs().all()).forEach(phantom.npcs()::unregister);
                    playerNpcs.clear();
                    send(p, SUCCESS_PREFIX + "Cleared " + count + " NPCs.");
                }
        ));
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
                    NPC<?> npc = playerNpcs.get(player.getUniqueId());
                    if (npc == null) yield List.of();
                    yield filter(
                            npc.supportedAnimations().stream()
                                    .map(prisons.solar.npclib.api.animation.NPCAnimation::name)
                                    .map(String::toLowerCase)
                                    .toList(),
                            args[1]
                    );
                }
                case "status" -> {
                    if (!(sender instanceof Player player)) yield List.of();
                    NPC<?> npc = playerNpcs.get(player.getUniqueId());
                    if (npc == null) yield List.of();
                    yield filter(
                            npc.supportedStatuses().stream()
                                    .map(prisons.solar.npclib.api.status.EntityStatus::name)
                                    .map(String::toLowerCase)
                                    .toList(),
                            args[1]
                    );
                }
                default -> List.of();
            };
        }
        return List.of();
    }

    private void sendHelp(Player player) {
        send(player, "§6§lPhantom NPC Test Commands");
        commands.forEach((name, cmd) ->
                send(player, "  §e/npctest " + cmd.usage + " §8- §7" + cmd.description)
        );
    }

    private void spawnNpc(Player player, EntityType type, String name) {
        if (playerNpcs.containsKey(player.getUniqueId())) {
            send(player, ERROR_PREFIX + "You already have an NPC. Use /npctest despawn first.");
            return;
        }

        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        loc.setY(player.getLocation().getY());

        NPC<?> npc = phantom.npcs().create(type, positionOf(loc));
        npc.appearance().setCustomName("§f" + name);
        npc.appearance().setCustomNameVisible(true);

        phantom.npcs().register(npc);
        playerNpcs.put(player.getUniqueId(), npc);

        send(player, SUCCESS_PREFIX + "Spawned §f" + type + " §anamed §f" + name);
    }

    private void despawnNpc(Player player) {
        NPC<?> npc = playerNpcs.remove(player.getUniqueId());
        if (npc == null) {
            send(player, ERROR_PREFIX + "You don't have an NPC.");
            return;
        }
        phantom.npcs().unregister(npc);
        send(player, SUCCESS_PREFIX + "Removed your NPC.");
    }

    private @Nullable NPC<?> getNpc(Player player) {
        NPC<?> npc = playerNpcs.get(player.getUniqueId());
        if (npc == null) {
            send(player, ERROR_PREFIX + "You don't have an NPC. Use /npctest spawn first.");
        }
        return npc;
    }

    private static Position positionOf(Location loc) {
        return Position.of(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
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

    private static prisons.solar.npclib.api.animation.NPCAnimation parseAnimation(String name, NPC<?> npc) {
        return npc.supportedAnimations().stream()
                .filter(anim -> anim.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown animation: " + name));
    }

    private static prisons.solar.npclib.api.status.EntityStatus parseStatus(String name, NPC<?> npc) {
        // Try CommonStatus first (universal statuses)
        try {
            return CommonStatus.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            // Not a common status, try entity-specific statuses
        }

        // Search through all supported statuses for entity-specific ones
        return npc.supportedStatuses().stream()
                .filter(status -> status.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown status: " + name));
    }

    private record SubCommand(String usage, String description, BiConsumer<Player, String[]> handler) {
        void execute(Player player, String[] args) {
            handler.accept(player, args);
        }
    }
}
