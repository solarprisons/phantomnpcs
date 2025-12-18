package prisons.solar.npclib.api.appearance;

import org.jetbrains.annotations.NotNull;

/**
 * Base interface for NPC appearance configuration.
 * Different entity types have different appearance implementations.
 */
public interface NPCAppearance {

    /**
     * Sets whether the NPC is glowing.
     *
     * @param glowing true to enable glow effect
     */
    void setGlowing(boolean glowing);

    /**
     * Returns whether the NPC is glowing.
     *
     * @return true if glowing
     */
    boolean isGlowing();

    /**
     * Sets whether the NPC is invisible.
     *
     * @param invisible true to make invisible
     */
    void setInvisible(boolean invisible);

    /**
     * Returns whether the NPC is invisible.
     *
     * @return true if invisible
     */
    boolean isInvisible();

    /**
     * Sets whether the NPC is on fire visually.
     *
     * @param onFire true to show fire effect
     */
    void setOnFire(boolean onFire);

    /**
     * Returns whether the NPC appears on fire.
     *
     * @return true if on fire
     */
    boolean isOnFire();

    /**
     * Sets whether the NPC has a custom name visible.
     *
     * @param visible true to show custom name
     */
    void setCustomNameVisible(boolean visible);

    /**
     * Returns whether the custom name is visible.
     *
     * @return true if visible
     */
    boolean isCustomNameVisible();

    /**
     * Sets the custom name of the NPC.
     * Supports MiniMessage format.
     *
     * @param name the name (MiniMessage format supported)
     */
    void setCustomName(@NotNull String name);

    /**
     * Gets the custom name of the NPC.
     *
     * @return the custom name
     */
    @NotNull String getCustomName();

    /**
     * Marks the appearance as dirty, triggering a sync to viewers.
     */
    void markDirty();
}
