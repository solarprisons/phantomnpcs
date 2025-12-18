package prisons.solar.npclib.api.appearance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Appearance configuration for item display entities.
 */
public interface ItemDisplayAppearance extends DisplayAppearance {

    /**
     * Item display transform modes.
     * Determines how the item is rendered.
     */
    enum DisplayTransform {
        NONE,
        THIRDPERSON_LEFTHAND,
        THIRDPERSON_RIGHTHAND,
        FIRSTPERSON_LEFTHAND,
        FIRSTPERSON_RIGHTHAND,
        HEAD,
        GUI,
        GROUND,
        FIXED
    }

    /**
     * Sets the displayed item.
     *
     * @param item the item data (platform-specific, e.g., ItemStack for Bukkit)
     */
    void setItem(@NotNull Object item);

    /**
     * Gets the displayed item data.
     *
     * @return the item data, or null if not set
     */
    @Nullable Object getItem();

    /**
     * Sets the item display transform.
     *
     * @param transform the display transform
     */
    void setDisplayTransform(@NotNull DisplayTransform transform);

    /**
     * Gets the item display transform.
     *
     * @return the display transform
     */
    @NotNull DisplayTransform getDisplayTransform();
}
