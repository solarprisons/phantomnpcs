package prisons.solar.npclib.paper.npc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType;
import me.tofaa.entitylib.meta.types.PlayerMeta;
import me.tofaa.entitylib.wrapper.WrapperPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.PlayerAppearance;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.npc.NPCState;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.viewer.Viewer;
import prisons.solar.npclib.core.event.SimpleEventBus;
import prisons.solar.npclib.core.event.SimpleNPCDespawnEvent;
import prisons.solar.npclib.core.event.SimpleNPCSpawnEvent;
import prisons.solar.npclib.core.npc.AbstractNPC;
import prisons.solar.npclib.paper.PaperViewer;
import prisons.solar.npclib.paper.skin.SkinManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Player NPC implementation using EntityLib.
 * Handles all packet-level details for player NPCs including:
 * - Skin application and fetching
 * - Tab list management (add briefly then remove)
 * - Equipment synchronization
 * - Metadata updates
 * - Animations
 */
public class EntityLibPlayerNPC extends AbstractNPC<PlayerAppearance> {

    private WrapperPlayer wrapperPlayer;
    private final EntityLibPlayerAppearance playerAppearance;

    private SkinManager skinManager;
    private SimpleEventBus eventBus;

    // Tab list configuration
    private static final long TAB_LIST_REMOVE_DELAY_MS = 50;
    private boolean removeFromTabList = true;

    public EntityLibPlayerNPC(@NotNull Position position) {
        super(EntityType.PLAYER, position);
        this.playerAppearance = new EntityLibPlayerAppearance(this);
        this.appearance = playerAppearance;
    }

    /**
     * Sets the skin manager for fetching skins.
     *
     * @param skinManager the skin manager
     */
    public void setSkinManager(@Nullable SkinManager skinManager) {
        this.skinManager = skinManager;
        this.playerAppearance.setSkinManager(skinManager);
    }

    /**
     * Sets the event bus for firing events.
     *
     * @param eventBus the event bus
     */
    public void setEventBus(@Nullable SimpleEventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Sets whether to remove this NPC from the tab list after spawning.
     *
     * @param remove true to remove from tab list
     */
    public void setRemoveFromTabList(boolean remove) {
        this.removeFromTabList = remove;
    }

    @Override
    public void spawn() {
        if (state != NPCState.REGISTERED && state != NPCState.DESPAWNED) {
            throw new IllegalStateException("NPC must be registered before spawning");
        }
        state = NPCState.SPAWNED;
    }

    @Override
    public void spawn(@NotNull Viewer viewer) {
        if (state == NPCState.UNREGISTERED || state == NPCState.DESTROYED) {
            return;
        }

        // Fire spawn event
        if (eventBus != null) {
            SimpleNPCSpawnEvent event = new SimpleNPCSpawnEvent(this, viewer);
            eventBus.post(event);
            if (event.isCancelled()) {
                return;
            }
        }

        if (!viewers.contains(viewer)) {
            addViewer(viewer);
            sendSpawnPackets(viewer);
        }
    }

    @Override
    public void despawn() {
        if (state == NPCState.DESTROYED) {
            return;
        }
        for (Viewer viewer : viewers) {
            // Fire despawn event
            if (eventBus != null) {
                eventBus.post(new SimpleNPCDespawnEvent(this, viewer));
            }
            sendDespawnPackets(viewer);
        }
        viewers.clear();
        state = NPCState.DESPAWNED;
    }

    @Override
    public void despawn(@NotNull Viewer viewer) {
        if (viewers.remove(viewer)) {
            // Fire despawn event
            if (eventBus != null) {
                eventBus.post(new SimpleNPCDespawnEvent(this, viewer));
            }
            sendDespawnPackets(viewer);
        }
    }

    @Override
    public void destroy() {
        despawn();
        state = NPCState.DESTROYED;
        if (wrapperPlayer != null) {
            wrapperPlayer.remove();
            wrapperPlayer = null;
        }
    }

    @Override
    public void teleport(@NotNull Position position) {
        this.position = position;
        sendTeleportPackets();
    }

    @Override
    public void lookAt(@NotNull Position target) {
        double dx = target.x() - position.x();
        double dy = target.y() - position.y();
        double dz = target.z() - position.z();

        double distXZ = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, distXZ));

        this.position = position.withRotation(yaw, pitch);
        sendLookPackets(yaw, pitch);
    }

    @Override
    public void lookAt(@NotNull Viewer viewer) {
        lookAt(viewer.position());
    }

    @Override
    public void playAnimation(@NotNull Animation animation) {
        for (Viewer viewer : viewers) {
            playAnimation(viewer, animation);
        }
    }

    @Override
    public void playAnimation(@NotNull Viewer viewer, @NotNull Animation animation) {
        if (wrapperPlayer != null && viewer instanceof PaperViewer) {
            EntityAnimationType animationType = switch (animation) {
                case SWING_MAIN_ARM -> EntityAnimationType.SWING_MAIN_ARM;
                case TAKE_DAMAGE -> EntityAnimationType.HURT;
                case LEAVE_BED -> EntityAnimationType.WAKE_UP;
                case SWING_OFFHAND -> EntityAnimationType.SWING_OFF_HAND;
                case CRITICAL_EFFECT -> EntityAnimationType.CRITICAL_HIT;
                case MAGIC_CRITICAL_EFFECT -> EntityAnimationType.MAGIC_CRITICAL_HIT;
            };
            wrapperPlayer.sendAnimation(animationType);
        }
    }

    @Override
    protected void onMetadataChanged() {
        syncMetadataToWrapper();
    }

    /**
     * Syncs all appearance/metadata to the EntityLib wrapper.
     */
    public void syncMetadataToWrapper() {
        if (wrapperPlayer == null) {
            return;
        }

        // Update player meta
        wrapperPlayer.consumeEntityMeta(PlayerMeta.class, meta -> {
            // Skin layers - set individual skin parts visibility
            byte skinMask = playerAppearance.getSkinLayerMask();
            meta.setCapeEnabled((skinMask & 0x01) != 0);
            meta.setJacketEnabled((skinMask & 0x02) != 0);
            meta.setLeftSleeveEnabled((skinMask & 0x04) != 0);
            meta.setRightSleeveEnabled((skinMask & 0x08) != 0);
            meta.setLeftLegEnabled((skinMask & 0x10) != 0);
            meta.setRightLegEnabled((skinMask & 0x20) != 0);
            meta.setHatEnabled((skinMask & 0x40) != 0);

            // Base entity flags
            meta.setOnFire(playerAppearance.isOnFire());
            meta.setInvisible(playerAppearance.isInvisible());
            meta.setGlowing(playerAppearance.isGlowing());

            // Custom name
            if (!playerAppearance.getCustomName().isEmpty()) {
                meta.setCustomName(Component.text(playerAppearance.getCustomName()));
                meta.setCustomNameVisible(playerAppearance.isCustomNameVisible());
            }

            // Pose
            meta.setPose(convertPose(playerAppearance.getPose()));
        });

        // Refresh metadata to viewers
        wrapperPlayer.refresh();
    }

    /**
     * Syncs equipment to all viewers.
     */
    public void syncEquipment() {
        if (wrapperPlayer == null) {
            return;
        }

        // EntityLib handles equipment through WrapperEntityEquipment
        var equipment = wrapperPlayer.getEquipment();

        // Convert our equipment to PacketEvents format
        for (PlayerAppearance.EquipmentSlot slot : PlayerAppearance.EquipmentSlot.values()) {
            Object item = playerAppearance.getEquipment(slot);
            if (item instanceof org.bukkit.inventory.ItemStack bukkitItem) {
                com.github.retrooper.packetevents.protocol.item.ItemStack peItem =
                        io.github.retrooper.packetevents.util.SpigotConversionUtil.fromBukkitItemStack(bukkitItem);

                com.github.retrooper.packetevents.protocol.player.EquipmentSlot peSlot = switch (slot) {
                    case MAIN_HAND -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.MAIN_HAND;
                    case OFF_HAND -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.OFF_HAND;
                    case HEAD -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.HELMET;
                    case CHEST -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.CHEST_PLATE;
                    case LEGS -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.LEGGINGS;
                    case FEET -> com.github.retrooper.packetevents.protocol.player.EquipmentSlot.BOOTS;
                };

                equipment.setItem(peSlot, peItem);
            }
        }
    }

    @Override
    protected void sendSpawnPackets(@NotNull Viewer viewer) {
        if (!(viewer instanceof PaperViewer paperViewer)) {
            return;
        }

        ensureWrapperPlayer();

        // Add viewer
        wrapperPlayer.addViewer(paperViewer.getPlayer().getUniqueId());

        // Spawn at location
        var location = new com.github.retrooper.packetevents.protocol.world.Location(
                position.x(), position.y(), position.z(), position.yaw(), position.pitch()
        );
        wrapperPlayer.spawn(location);

        // Schedule tab list removal if configured
        if (removeFromTabList) {
            CompletableFuture.delayedExecutor(TAB_LIST_REMOVE_DELAY_MS, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        if (wrapperPlayer != null) {
                            wrapperPlayer.setInTablist(false);
                            // Send tab list remove packet
                            var removePacket = wrapperPlayer.tabListRemovePacket();
                            if (removePacket != null) {
                                PacketEvents.getAPI().getPlayerManager()
                                        .sendPacket(paperViewer.getPlayer(), removePacket);
                            }
                        }
                    });
        }
    }

    @Override
    protected void sendDespawnPackets(@NotNull Viewer viewer) {
        if (wrapperPlayer != null && viewer instanceof PaperViewer paperViewer) {
            wrapperPlayer.removeViewer(paperViewer.getPlayer().getUniqueId());
        }
    }

    @Override
    protected void sendTeleportPackets() {
        if (wrapperPlayer != null) {
            var location = new com.github.retrooper.packetevents.protocol.world.Location(
                    position.x(), position.y(), position.z(), position.yaw(), position.pitch()
            );
            wrapperPlayer.teleport(location);
        }
    }

    @Override
    protected void sendLookPackets(float yaw, float pitch) {
        if (wrapperPlayer != null) {
            wrapperPlayer.rotateHead(yaw, pitch);
        }
    }

    private void ensureWrapperPlayer() {
        if (wrapperPlayer == null) {
            // Create user profile with skin if available
            UserProfile profile = new UserProfile(id, playerAppearance.getDisplayName());

            // Apply skin textures
            PlayerAppearance.Skin skin = playerAppearance.getSkin();
            if (skin != null) {
                List<TextureProperty> textures = SkinManager.toTextureProperties(skin);
                profile = new UserProfile(id, playerAppearance.getDisplayName(), textures);
            }

            wrapperPlayer = new WrapperPlayer(profile, entityId);
            wrapperPlayer.setInTablist(true);

            // Sync metadata
            syncMetadataToWrapper();
        }
    }

    private com.github.retrooper.packetevents.protocol.entity.pose.EntityPose convertPose(
            PlayerAppearance.EntityPose pose) {
        return switch (pose) {
            case STANDING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.STANDING;
            case FALL_FLYING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.FALL_FLYING;
            case SLEEPING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.SLEEPING;
            case SWIMMING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.SWIMMING;
            case SPIN_ATTACK -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.SPIN_ATTACK;
            case SNEAKING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.CROUCHING;
            case DYING -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.DYING;
            default -> com.github.retrooper.packetevents.protocol.entity.pose.EntityPose.STANDING;
        };
    }

    /**
     * Gets the EntityLib wrapper player.
     *
     * @return the wrapper player
     */
    public WrapperPlayer getWrapperPlayer() {
        return wrapperPlayer;
    }

    /**
     * Updates the skin and respawns for all viewers.
     *
     * @param skin the new skin
     */
    public void updateSkin(@NotNull PlayerAppearance.Skin skin) {
        playerAppearance.setSkin(skin);

        if (wrapperPlayer != null) {
            // Update textures
            wrapperPlayer.setTextureProperties(SkinManager.toTextureProperties(skin));

            // Respawn for all viewers to show new skin
            for (Viewer viewer : viewers) {
                if (viewer instanceof PaperViewer paperViewer) {
                    // Remove and re-add to refresh skin
                    wrapperPlayer.removeViewer(paperViewer.getPlayer().getUniqueId());
                    wrapperPlayer.addViewer(paperViewer.getPlayer().getUniqueId());

                    // Re-send spawn packets
                    var location = new com.github.retrooper.packetevents.protocol.world.Location(
                            position.x(), position.y(), position.z(), position.yaw(), position.pitch()
                    );
                    wrapperPlayer.spawn(location);

                    // Schedule tab removal again
                    if (removeFromTabList) {
                        CompletableFuture.delayedExecutor(TAB_LIST_REMOVE_DELAY_MS, TimeUnit.MILLISECONDS)
                                .execute(() -> {
                                    if (wrapperPlayer != null) {
                                        wrapperPlayer.setInTablist(false);
                                        var removePacket = wrapperPlayer.tabListRemovePacket();
                                        if (removePacket != null) {
                                            PacketEvents.getAPI().getPlayerManager()
                                                    .sendPacket(paperViewer.getPlayer(), removePacket);
                                        }
                                    }
                                });
                    }
                }
            }
        }
    }
}
