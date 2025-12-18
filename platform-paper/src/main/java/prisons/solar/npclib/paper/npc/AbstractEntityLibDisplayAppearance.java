package prisons.solar.npclib.paper.npc;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.appearance.DisplayAppearance;

/**
 * Abstract base implementation for display entity appearance.
 *
 * @param <N> the NPC type
 */
public abstract class AbstractEntityLibDisplayAppearance<N extends AbstractEntityLibDisplayNPC<?>> implements DisplayAppearance {

    protected final N npc;

    protected boolean onFire;
    protected boolean invisible;
    protected boolean glowing;
    protected String customName = "";
    protected boolean customNameVisible;

    protected BillboardMode billboardMode = BillboardMode.FIXED;
    protected int interpolationDuration = 0;
    protected int interpolationDelay = 0;
    protected float viewRange = 1.0f;
    protected float shadowRadius = 0f;
    protected float shadowStrength = 1.0f;
    protected float displayWidth = 0f;
    protected float displayHeight = 0f;
    protected Integer blockBrightness;
    protected Integer skyBrightness;
    protected Integer glowColorOverride;

    protected float translationX = 0f, translationY = 0f, translationZ = 0f;
    protected float scaleX = 1f, scaleY = 1f, scaleZ = 1f;
    protected float leftRotationX = 0f, leftRotationY = 0f, leftRotationZ = 0f, leftRotationW = 1f;
    protected float rightRotationX = 0f, rightRotationY = 0f, rightRotationZ = 0f, rightRotationW = 1f;

    protected AbstractEntityLibDisplayAppearance(@NotNull N npc) {
        this.npc = npc;
    }

    @Override
    public void setOnFire(boolean onFire) {
        this.onFire = onFire;
        npc.onMetadataChanged();
    }

    @Override
    public boolean isOnFire() {
        return onFire;
    }

    @Override
    public void setInvisible(boolean invisible) {
        this.invisible = invisible;
        npc.onMetadataChanged();
    }

    @Override
    public boolean isInvisible() {
        return invisible;
    }

    @Override
    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
        npc.onMetadataChanged();
    }

    @Override
    public boolean isGlowing() {
        return glowing;
    }

    @Override
    public void setCustomName(@NotNull String name) {
        this.customName = name;
        npc.onMetadataChanged();
    }

    @Override
    public @NotNull String getCustomName() {
        return customName;
    }

    @Override
    public void setCustomNameVisible(boolean visible) {
        this.customNameVisible = visible;
        npc.onMetadataChanged();
    }

    @Override
    public boolean isCustomNameVisible() {
        return customNameVisible;
    }

    @Override
    public void setBillboardMode(@NotNull BillboardMode mode) {
        this.billboardMode = mode;
        npc.onMetadataChanged();
    }

    @Override
    public @NotNull BillboardMode getBillboardMode() {
        return billboardMode;
    }

    @Override
    public void setInterpolationDuration(int ticks) {
        this.interpolationDuration = ticks;
        npc.onMetadataChanged();
    }

    @Override
    public int getInterpolationDuration() {
        return interpolationDuration;
    }

    @Override
    public void setInterpolationDelay(int ticks) {
        this.interpolationDelay = ticks;
        npc.onMetadataChanged();
    }

    @Override
    public int getInterpolationDelay() {
        return interpolationDelay;
    }

    @Override
    public void setViewRange(float range) {
        this.viewRange = range;
        npc.onMetadataChanged();
    }

    @Override
    public float getViewRange() {
        return viewRange;
    }

    @Override
    public void setShadowRadius(float radius) {
        this.shadowRadius = radius;
        npc.onMetadataChanged();
    }

    @Override
    public float getShadowRadius() {
        return shadowRadius;
    }

    @Override
    public void setShadowStrength(float strength) {
        this.shadowStrength = strength;
        npc.onMetadataChanged();
    }

    @Override
    public float getShadowStrength() {
        return shadowStrength;
    }

    @Override
    public void setDisplayWidth(float width) {
        this.displayWidth = width;
        npc.onMetadataChanged();
    }

    @Override
    public float getDisplayWidth() {
        return displayWidth;
    }

    @Override
    public void setDisplayHeight(float height) {
        this.displayHeight = height;
        npc.onMetadataChanged();
    }

    @Override
    public float getDisplayHeight() {
        return displayHeight;
    }

    @Override
    public void setBrightness(@Nullable Integer block, @Nullable Integer sky) {
        this.blockBrightness = block;
        this.skyBrightness = sky;
        npc.onMetadataChanged();
    }

    @Override
    public @Nullable Integer getBlockBrightness() {
        return blockBrightness;
    }

    @Override
    public @Nullable Integer getSkyBrightness() {
        return skyBrightness;
    }

    @Override
    public void setGlowColorOverride(@Nullable Integer color) {
        this.glowColorOverride = color;
        npc.onMetadataChanged();
    }

    @Override
    public @Nullable Integer getGlowColorOverride() {
        return glowColorOverride;
    }

    @Override
    public void setTranslation(float x, float y, float z) {
        this.translationX = x;
        this.translationY = y;
        this.translationZ = z;
        npc.onMetadataChanged();
    }

    @Override
    public void setScale(float x, float y, float z) {
        this.scaleX = x;
        this.scaleY = y;
        this.scaleZ = z;
        npc.onMetadataChanged();
    }

    @Override
    public void setLeftRotation(float x, float y, float z, float w) {
        this.leftRotationX = x;
        this.leftRotationY = y;
        this.leftRotationZ = z;
        this.leftRotationW = w;
        npc.onMetadataChanged();
    }

    @Override
    public void setRightRotation(float x, float y, float z, float w) {
        this.rightRotationX = x;
        this.rightRotationY = y;
        this.rightRotationZ = z;
        this.rightRotationW = w;
        npc.onMetadataChanged();
    }

    // Getters for internal use
    public float getTranslationX() { return translationX; }
    public float getTranslationY() { return translationY; }
    public float getTranslationZ() { return translationZ; }
    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public float getScaleZ() { return scaleZ; }
    public float getLeftRotationX() { return leftRotationX; }
    public float getLeftRotationY() { return leftRotationY; }
    public float getLeftRotationZ() { return leftRotationZ; }
    public float getLeftRotationW() { return leftRotationW; }
    public float getRightRotationX() { return rightRotationX; }
    public float getRightRotationY() { return rightRotationY; }
    public float getRightRotationZ() { return rightRotationZ; }
    public float getRightRotationW() { return rightRotationW; }

    @Override
    public void markDirty() {
        npc.onMetadataChanged();
    }
}
