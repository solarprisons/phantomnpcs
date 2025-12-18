package prisons.solar.npclib.paper.npc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation.EntityAnimationType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.types.PlayerMeta;
import me.tofaa.entitylib.wrapper.WrapperPlayer;
import net.kyori.adventure.text.Component;
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

public class EntityLibPlayerNPC extends AbstractNPC<PlayerAppearance> {

    private WrapperPlayer wrapperPlayer;
    private final EntityLibPlayerAppearance playerAppearance;

    private SkinManager skinManager;
    private SimpleEventBus eventBus;

    private static final long TAB_LIST_REMOVE_DELAY_MS = 50;
    private boolean removeFromTabList = true;

    public EntityLibPlayerNPC(@NotNull Position position) {
        super(EntityType.PLAYER, position);
        this.playerAppearance = new EntityLibPlayerAppearance(this);
        this.appearance = playerAppearance;
    }

    public void setSkinManager(@Nullable SkinManager skinManager) {
        this.skinManager = skinManager;
        this.playerAppearance.setSkinManager(skinManager);
    }

    public void setEventBus(@Nullable SimpleEventBus eventBus) {
        this.eventBus = eventBus;
    }

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

    public void syncMetadataToWrapper() {
        if (wrapperPlayer == null) {
            return;
        }

        wrapperPlayer.consumeEntityMeta(PlayerMeta.class, meta -> {
            // Skip EntityLib's skin layer methods - they use wrong indices for 1.21.x
            // We'll send skin parts manually below with correct index 16

            meta.setOnFire(playerAppearance.isOnFire());
            meta.setInvisible(playerAppearance.isInvisible());
            meta.setGlowing(playerAppearance.isGlowing());

            if (!playerAppearance.getCustomName().isEmpty()) {
                meta.setCustomName(Component.text(playerAppearance.getCustomName()));
                meta.setCustomNameVisible(playerAppearance.isCustomNameVisible());
            }

            meta.setPose(convertPose(playerAppearance.getPose()));
        });

        wrapperPlayer.refresh();
    }

    /**
     * Sends skin layer metadata directly using PacketEvents with correct index.
     * EntityLib uses wrong indices for 1.21.x (sends to index 17 instead of 16).
     */
    public void sendSkinLayersPacket() {
        if (wrapperPlayer == null) {
            return;
        }

        byte skinMask = playerAppearance.getSkinLayerMask();
        // Index 16 = Displayed Skin Parts (Byte) per MC protocol wiki
        EntityData skinData = new EntityData(16, EntityDataTypes.BYTE, skinMask);
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(
                entityId, List.of(skinData)
        );

        for (Viewer viewer : viewers) {
            if (viewer instanceof PaperViewer paperViewer) {
                PacketEvents.getAPI().getPlayerManager()
                        .sendPacket(paperViewer.getPlayer(), packet);
            }
        }
    }

    public void syncEquipment() {
        if (wrapperPlayer == null) {
            return;
        }

        var equipment = wrapperPlayer.getEquipment();

        for (PlayerAppearance.EquipmentSlot slot : PlayerAppearance.EquipmentSlot.values()) {
            Object item = playerAppearance.getEquipment(slot);
            if (item instanceof org.bukkit.inventory.ItemStack bukkitItem) {
                ItemStack peItem = SpigotConversionUtil.fromBukkitItemStack(bukkitItem);

                EquipmentSlot peSlot = switch (slot) {
                    case MAIN_HAND -> EquipmentSlot.MAIN_HAND;
                    case OFF_HAND -> EquipmentSlot.OFF_HAND;
                    case HEAD -> EquipmentSlot.HELMET;
                    case CHEST -> EquipmentSlot.CHEST_PLATE;
                    case LEGS -> EquipmentSlot.LEGGINGS;
                    case FEET -> EquipmentSlot.BOOTS;
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

        Location location = new Location(
                position.x(), position.y(), position.z(), position.yaw(), position.pitch()
        );

        // Spawn the entity first if not already spawned
        if (!wrapperPlayer.isSpawned()) {
            wrapperPlayer.spawn(location);
        }

        // Add viewer - EntityLib will send spawn packets to this viewer
        wrapperPlayer.addViewer(paperViewer.getPlayer().getUniqueId());

        // Send skin layers with correct metadata index (16) - EntityLib uses wrong index
        byte skinMask = playerAppearance.getSkinLayerMask();
        EntityData skinData = new EntityData(16, EntityDataTypes.BYTE, skinMask);
        WrapperPlayServerEntityMetadata skinPacket = new WrapperPlayServerEntityMetadata(
                entityId, List.of(skinData)
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(paperViewer.getPlayer(), skinPacket);

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

    @Override
    protected void sendDespawnPackets(@NotNull Viewer viewer) {
        if (wrapperPlayer != null && viewer instanceof PaperViewer paperViewer) {
            wrapperPlayer.removeViewer(paperViewer.getPlayer().getUniqueId());
        }
    }

    @Override
    protected void sendTeleportPackets() {
        if (wrapperPlayer != null) {
            Location location = new Location(
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
            UserProfile profile = new UserProfile(id, playerAppearance.getDisplayName());

            PlayerAppearance.Skin skin = playerAppearance.getSkin();
            if (skin != null) {
                List<TextureProperty> textures = SkinManager.toTextureProperties(skin);
                profile = new UserProfile(id, playerAppearance.getDisplayName(), textures);
            }

            wrapperPlayer = new WrapperPlayer(profile, entityId);
            wrapperPlayer.setInTablist(true);

            syncMetadataToWrapper();
        }
    }

    private EntityPose convertPose(PlayerAppearance.EntityPose pose) {
        return switch (pose) {
            case STANDING -> EntityPose.STANDING;
            case FALL_FLYING -> EntityPose.FALL_FLYING;
            case SLEEPING -> EntityPose.SLEEPING;
            case SWIMMING -> EntityPose.SWIMMING;
            case SPIN_ATTACK -> EntityPose.SPIN_ATTACK;
            case SNEAKING -> EntityPose.CROUCHING;
            case DYING -> EntityPose.DYING;
            default -> EntityPose.STANDING;
        };
    }

    public WrapperPlayer getWrapperPlayer() {
        return wrapperPlayer;
    }

    public void updateSkin(@NotNull PlayerAppearance.Skin skin) {
        playerAppearance.setSkin(skin);

        if (wrapperPlayer != null) {
            wrapperPlayer.setTextureProperties(SkinManager.toTextureProperties(skin));

            // Re-spawn for all viewers to apply the skin change
            for (Viewer viewer : viewers) {
                if (viewer instanceof PaperViewer paperViewer) {
                    // Remove and re-add viewer to trigger respawn with new skin
                    wrapperPlayer.removeViewer(paperViewer.getPlayer().getUniqueId());
                    wrapperPlayer.addViewer(paperViewer.getPlayer().getUniqueId());

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
