package prisons.solar.npclib.api.appearance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base appearance configuration for display entities (1.19.4+).
 * Display entities include text displays, block displays, and item displays.
 */
public interface DisplayAppearance extends NPCAppearance {

    /**
     * Billboard constraint modes.
     * Determines how the display rotates relative to the viewer.
     */
    enum BillboardMode {
        /** No billboard - displays stay at their set rotation */
        FIXED,
        /** Rotates vertically to face the player (pivot around Y axis) */
        VERTICAL,
        /** Rotates horizontally to face the player (pivot around X axis) */
        HORIZONTAL,
        /** Fully rotates to face the player (both axes) */
        CENTER
    }

    /**
     * Sets the billboard constraint mode.
     *
     * @param mode the billboard mode
     */
    void setBillboardMode(@NotNull BillboardMode mode);

    /**
     * Gets the billboard constraint mode.
     *
     * @return the billboard mode
     */
    @NotNull BillboardMode getBillboardMode();

    /**
     * Sets the interpolation duration in ticks.
     * This controls how long it takes for changes to animate.
     *
     * @param ticks duration in ticks
     */
    void setInterpolationDuration(int ticks);

    /**
     * Gets the interpolation duration.
     *
     * @return duration in ticks
     */
    int getInterpolationDuration();

    /**
     * Sets the interpolation start delay in ticks.
     *
     * @param ticks delay in ticks
     */
    void setInterpolationDelay(int ticks);

    /**
     * Gets the interpolation start delay.
     *
     * @return delay in ticks
     */
    int getInterpolationDelay();

    /**
     * Sets the view range multiplier.
     * Default is 1.0, higher values increase view distance.
     *
     * @param range view range multiplier
     */
    void setViewRange(float range);

    /**
     * Gets the view range multiplier.
     *
     * @return view range multiplier
     */
    float getViewRange();

    /**
     * Sets the shadow radius.
     * Set to 0 to disable shadow.
     *
     * @param radius shadow radius
     */
    void setShadowRadius(float radius);

    /**
     * Gets the shadow radius.
     *
     * @return shadow radius
     */
    float getShadowRadius();

    /**
     * Sets the shadow strength (opacity).
     *
     * @param strength shadow strength (0.0-1.0)
     */
    void setShadowStrength(float strength);

    /**
     * Gets the shadow strength.
     *
     * @return shadow strength
     */
    float getShadowStrength();

    /**
     * Sets the display width for culling.
     *
     * @param width display width
     */
    void setDisplayWidth(float width);

    /**
     * Gets the display width.
     *
     * @return display width
     */
    float getDisplayWidth();

    /**
     * Sets the display height for culling.
     *
     * @param height display height
     */
    void setDisplayHeight(float height);

    /**
     * Gets the display height.
     *
     * @return display height
     */
    float getDisplayHeight();

    /**
     * Sets the brightness override.
     * Null uses natural lighting.
     *
     * @param block block light level (0-15), or null to use natural
     * @param sky sky light level (0-15), or null to use natural
     */
    void setBrightness(@Nullable Integer block, @Nullable Integer sky);

    /**
     * Gets the block brightness override.
     *
     * @return block brightness or null
     */
    @Nullable Integer getBlockBrightness();

    /**
     * Gets the sky brightness override.
     *
     * @return sky brightness or null
     */
    @Nullable Integer getSkyBrightness();

    /**
     * Sets the glow color override (ARGB).
     * Null uses default glow color.
     *
     * @param color ARGB color, or null for default
     */
    void setGlowColorOverride(@Nullable Integer color);

    /**
     * Gets the glow color override.
     *
     * @return ARGB color or null
     */
    @Nullable Integer getGlowColorOverride();

    /**
     * Sets the translation (position offset).
     *
     * @param x x offset
     * @param y y offset
     * @param z z offset
     */
    void setTranslation(float x, float y, float z);

    /**
     * Sets the scale.
     *
     * @param x x scale
     * @param y y scale
     * @param z z scale
     */
    void setScale(float x, float y, float z);

    /**
     * Sets the left rotation (quaternion).
     *
     * @param x x component
     * @param y y component
     * @param z z component
     * @param w w component
     */
    void setLeftRotation(float x, float y, float z, float w);

    /**
     * Sets the right rotation (quaternion).
     *
     * @param x x component
     * @param y y component
     * @param z z component
     * @param w w component
     */
    void setRightRotation(float x, float y, float z, float w);
}
