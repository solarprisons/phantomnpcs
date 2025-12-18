package prisons.solar.npclib.api.appearance;

import org.jetbrains.annotations.NotNull;

/**
 * Appearance configuration for text display entities.
 */
public interface TextDisplayAppearance extends DisplayAppearance {

    /**
     * Text alignment options.
     */
    enum TextAlignment {
        CENTER,
        LEFT,
        RIGHT
    }

    /**
     * Sets the displayed text.
     *
     * @param text the text to display (supports color codes)
     */
    void setText(@NotNull String text);

    /**
     * Gets the displayed text.
     *
     * @return the text
     */
    @NotNull String getText();

    /**
     * Sets the line width before wrapping.
     *
     * @param width line width in pixels
     */
    void setLineWidth(int width);

    /**
     * Gets the line width.
     *
     * @return line width in pixels
     */
    int getLineWidth();

    /**
     * Sets the text background color (ARGB).
     * Use 0 for no background.
     *
     * @param color ARGB color value
     */
    void setBackgroundColor(int color);

    /**
     * Gets the text background color.
     *
     * @return ARGB color value
     */
    int getBackgroundColor();

    /**
     * Sets the text opacity.
     *
     * @param opacity opacity (0-255, where 255 is fully opaque)
     */
    void setTextOpacity(int opacity);

    /**
     * Gets the text opacity.
     *
     * @return opacity value
     */
    int getTextOpacity();

    /**
     * Sets whether the text has shadow.
     *
     * @param shadow true for shadow
     */
    void setShadow(boolean shadow);

    /**
     * Returns whether the text has shadow.
     *
     * @return true if has shadow
     */
    boolean hasShadow();

    /**
     * Sets whether to render the text see-through (visible through blocks).
     *
     * @param seeThrough true for see-through
     */
    void setSeeThrough(boolean seeThrough);

    /**
     * Returns whether the text is see-through.
     *
     * @return true if see-through
     */
    boolean isSeeThrough();

    /**
     * Sets whether to use the default background.
     *
     * @param useDefault true to use default background
     */
    void setDefaultBackground(boolean useDefault);

    /**
     * Returns whether using the default background.
     *
     * @return true if using default background
     */
    boolean hasDefaultBackground();

    /**
     * Sets the text alignment.
     *
     * @param alignment the alignment
     */
    void setAlignment(@NotNull TextAlignment alignment);

    /**
     * Gets the text alignment.
     *
     * @return the alignment
     */
    @NotNull TextAlignment getAlignment();
}
