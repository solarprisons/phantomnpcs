package prisons.solar.npclib.paper.listener;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.core.event.SimpleNPCInteractEvent;
import prisons.solar.npclib.api.interaction.InteractionHandler;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.NPCRegistry;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.core.event.SimpleEventBus;
import prisons.solar.npclib.core.npc.AbstractNPC;
import prisons.solar.npclib.paper.PaperPlatformAdapter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for entity interaction packets to detect NPC clicks.
 */
public class InteractionListener extends PacketListenerAbstract {

    private final Plugin plugin;
    private final NPCRegistry registry;
    private final PaperPlatformAdapter platform;
    private final SimpleEventBus eventBus;

    // Cooldown to prevent spam clicking
    private final Map<UUID, Long> clickCooldowns = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 100;

    public InteractionListener(@NotNull Plugin plugin,
                                @NotNull NPCRegistry registry,
                                @NotNull PaperPlatformAdapter platform,
                                @NotNull SimpleEventBus eventBus) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        this.registry = registry;
        this.platform = platform;
        this.eventBus = eventBus;
    }

    /**
     * Registers this listener with PacketEvents.
     */
    public void register() {
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    /**
     * Unregisters this listener from PacketEvents.
     */
    public void unregister() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) {
            return;
        }

        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        int entityId = packet.getEntityId();

        // Check if this is one of our NPCs
        registry.byEntityId(entityId).ifPresent(npc -> {
            Player player = (Player) event.getPlayer();
            UUID playerId = player.getUniqueId();

            // Apply cooldown
            Long lastClick = clickCooldowns.get(playerId);
            long now = System.currentTimeMillis();
            if (lastClick != null && now - lastClick < COOLDOWN_MS) {
                event.setCancelled(true);
                return;
            }
            clickCooldowns.put(playerId, now);

            // Cancel the packet - NPCs don't exist server-side
            event.setCancelled(true);

            // Get viewer
            Viewer viewer = platform.viewer(playerId);
            if (viewer == null) {
                return;
            }

            // Determine click type
            WrapperPlayClientInteractEntity.InteractAction action = packet.getAction();
            InteractionHandler.ClickType clickType = switch (action) {
                case INTERACT -> InteractionHandler.ClickType.RIGHT_CLICK;
                case ATTACK -> InteractionHandler.ClickType.LEFT_CLICK;
                case INTERACT_AT -> {
                    // INTERACT_AT is right-click at specific position
                    yield InteractionHandler.ClickType.RIGHT_CLICK;
                }
            };

            // Determine hand
            InteractionHandler.Hand hand = InteractionHandler.Hand.MAIN_HAND;
            if (action == WrapperPlayClientInteractEntity.InteractAction.INTERACT ||
                action == WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
                InteractionHand packetHand = packet.getHand();
                if (packetHand == InteractionHand.OFF_HAND) {
                    hand = InteractionHandler.Hand.OFF_HAND;
                }
            }

            // Create context
            InteractionHandler.Context context = new InteractionHandler.Context(
                    npc,
                    viewer,
                    clickType,
                    hand
            );

            // Run on main thread
            final InteractionHandler.Hand finalHand = hand;
            final InteractionHandler.ClickType finalClickType = clickType;

            Bukkit.getScheduler().runTask(plugin, () -> {
                // Fire event
                SimpleNPCInteractEvent interactEvent = new SimpleNPCInteractEvent(npc, viewer, finalClickType, finalHand);
                eventBus.post(interactEvent);

                if (interactEvent.isCancelled()) {
                    return;
                }

                // Call handler if set
                if (npc instanceof AbstractNPC<?> abstractNpc) {
                    InteractionHandler handler = abstractNpc.getInteractionHandler();
                    if (handler != null) {
                        try {
                            handler.handle(context);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error in NPC interaction handler: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            });
        });
    }

    /**
     * Clears click cooldowns.
     */
    public void clearCooldowns() {
        clickCooldowns.clear();
    }
}
