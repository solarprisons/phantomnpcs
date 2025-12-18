package prisons.solar.npclib.api.appearance;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Appearance configuration for block display entities.
 */
public interface BlockDisplayAppearance extends DisplayAppearance {

    /**
     * Sets the displayed block.
     *
     * @param blockData the block data (platform-specific, e.g., BlockData for Bukkit)
     */
    void setBlock(@NotNull Object blockData);

    /**
     * Gets the displayed block data.
     *
     * @return the block data, or null if not set
     */
    @Nullable Object getBlock();
}
