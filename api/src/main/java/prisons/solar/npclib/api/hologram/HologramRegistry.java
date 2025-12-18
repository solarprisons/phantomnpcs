package prisons.solar.npclib.api.hologram;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.Position;

import java.util.Collection;
import java.util.Optional;

/**
 * Registry for managing holograms.
 */
public interface HologramRegistry {

    /**
     * Creates a new hologram.
     *
     * @param id       unique identifier
     * @param position the position
     * @return the created hologram
     */
    @NotNull Hologram create(@NotNull String id, @NotNull Position position);

    /**
     * Gets a hologram by ID.
     *
     * @param id the hologram ID
     * @return the hologram if found
     */
    @NotNull Optional<Hologram> byId(@NotNull String id);

    /**
     * Gets all holograms.
     *
     * @return collection of holograms
     */
    @NotNull Collection<Hologram> all();

    /**
     * Gets all holograms in a world.
     *
     * @param worldId the world ID
     * @return collection of holograms
     */
    @NotNull Collection<Hologram> inWorld(@NotNull String worldId);

    /**
     * Removes a hologram.
     *
     * @param hologram the hologram to remove
     */
    void remove(@NotNull Hologram hologram);

    /**
     * Removes all holograms.
     */
    void clear();

    /**
     * Gets the count of holograms.
     *
     * @return hologram count
     */
    int count();
}
