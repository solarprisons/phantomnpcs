package prisons.solar.npclib.paper.npc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.PlayerAppearance;
import prisons.solar.npclib.paper.skin.SkinManager;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Player appearance implementation for EntityLib-based NPCs.
 * Handles all appearance-related state and syncs changes to the underlying NPC.
 */
public class EntityLibPlayerAppearance implements PlayerAppearance {

    private final EntityLibPlayerNPC npc;
    private SkinManager skinManager;

    private Skin skin;
    private String customName = "";
    private boolean customNameVisible = false;
    private boolean glowing = false;
    private boolean invisible = false;
    private boolean onFire = false;
    private EntityPose pose = EntityPose.STANDING;
    private final Object[] equipment = new Object[6];
    private final Set<SkinLayer> visibleSkinLayers = EnumSet.allOf(SkinLayer.class);

    public EntityLibPlayerAppearance(@NotNull EntityLibPlayerNPC npc) {
        this.npc = npc;
    }

    /**
     * Sets the skin manager for async skin fetching.
     *
     * @param skinManager the skin manager
     */
    public void setSkinManager(@Nullable SkinManager skinManager) {
        this.skinManager = skinManager;
    }

    // Base appearance methods

    @Override
    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
        markDirty();
    }

    @Override
    public boolean isGlowing() {
        return glowing;
    }

    @Override
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
        markDirty();
    }

    @Override
    public boolean isInvisible() {
        return invisible;
    }

    @Override
    public void setOnFire(boolean onFire) {
        this.onFire = onFire;
        markDirty();
    }

    @Override
    public boolean isOnFire() {
        return onFire;
    }

    @Override
    public void setCustomNameVisible(boolean visible) {
        this.customNameVisible = visible;
        markDirty();
    }

    @Override
    public boolean isCustomNameVisible() {
        return customNameVisible;
    }

    @Override
    public void setCustomName(@NotNull String name) {
        this.customName = name;
        markDirty();
    }

    @Override
    public @NotNull String getCustomName() {
        return customName;
    }

    // Living appearance methods

    @Override
    public void setEquipment(@NotNull EquipmentSlot slot, @Nullable Object item) {
        equipment[slot.ordinal()] = item;
        npc.syncEquipment();
    }

    @Override
    public @Nullable Object getEquipment(@NotNull EquipmentSlot slot) {
        return equipment[slot.ordinal()];
    }

    @Override
    public void clearEquipment() {
        for (int i = 0; i < equipment.length; i++) {
            equipment[i] = null;
        }
        npc.syncEquipment();
    }

    @Override
    public void setPose(@NotNull EntityPose pose) {
        this.pose = pose;
        markDirty();
    }

    @Override
    public @NotNull EntityPose getPose() {
        return pose;
    }

    // Player-specific appearance methods

    @Override
    public void setSkin(@NotNull Skin skin) {
        this.skin = skin;
        // Don't call markDirty - skin changes require respawn
        // The NPC handles this via updateSkin()
    }

    @Override
    public @Nullable Skin getSkin() {
        return skin;
    }

    @Override
    public CompletableFuture<Void> setSkinFromUUID(@NotNull UUID uuid) {
        if (skinManager == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("SkinManager not configured"));
        }

        return skinManager.fetchByUUID(uuid)
                .thenAccept(fetchedSkin -> {
                    if (fetchedSkin != null) {
                        npc.updateSkin(fetchedSkin);
                    }
                });
    }

    @Override
    public CompletableFuture<Void> setSkinFromName(@NotNull String name) {
        if (skinManager == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("SkinManager not configured"));
        }

        return skinManager.fetchByName(name)
                .thenAccept(fetchedSkin -> {
                    if (fetchedSkin != null) {
                        npc.updateSkin(fetchedSkin);
                    }
                });
    }

    @Override
    public void setSkinLayerVisible(@NotNull SkinLayer layer, boolean visible) {
        if (visible) {
            visibleSkinLayers.add(layer);
        } else {
            visibleSkinLayers.remove(layer);
        }
        markDirty();
    }

    @Override
    public boolean isSkinLayerVisible(@NotNull SkinLayer layer) {
        return visibleSkinLayers.contains(layer);
    }

    @Override
    public void setAllSkinLayersVisible() {
        visibleSkinLayers.addAll(EnumSet.allOf(SkinLayer.class));
        markDirty();
    }

    @Override
    public void markDirty() {
        npc.syncMetadataToWrapper();
    }

    /**
     * Gets the display name for the NPC.
     *
     * @return display name or default
     */
    public String getDisplayName() {
        return customName.isEmpty() ? "NPC" : customName;
    }

    /**
     * Gets the skin layer byte mask for protocol.
     *
     * @return skin layer mask
     */
    public byte getSkinLayerMask() {
        byte mask = 0;
        if (visibleSkinLayers.contains(SkinLayer.CAPE)) mask |= 0x01;
        if (visibleSkinLayers.contains(SkinLayer.JACKET)) mask |= 0x02;
        if (visibleSkinLayers.contains(SkinLayer.LEFT_SLEEVE)) mask |= 0x04;
        if (visibleSkinLayers.contains(SkinLayer.RIGHT_SLEEVE)) mask |= 0x08;
        if (visibleSkinLayers.contains(SkinLayer.LEFT_PANTS)) mask |= 0x10;
        if (visibleSkinLayers.contains(SkinLayer.RIGHT_PANTS)) mask |= 0x20;
        if (visibleSkinLayers.contains(SkinLayer.HAT)) mask |= 0x40;
        return mask;
    }
}
