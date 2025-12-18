package prisons.solar.npclib.api.hologram;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.viewer.Viewer;

import java.util.Collection;
import java.util.List;

/**
 * Represents a multi-line hologram (floating text display).
 */
public interface Hologram {

    /**
     * Gets the unique identifier for this hologram.
     *
     * @return the identifier
     */
    @NotNull String id();

    /**
     * Gets the position of this hologram.
     *
     * @return the position
     */
    @NotNull Position position();

    /**
     * Sets the position of this hologram.
     *
     * @param position the new position
     */
    void setPosition(@NotNull Position position);

    /**
     * Gets all lines of this hologram.
     *
     * @return list of lines
     */
    @NotNull List<String> lines();

    /**
     * Sets all lines of this hologram.
     *
     * @param lines the lines
     */
    void setLines(@NotNull List<String> lines);

    /**
     * Sets a specific line.
     *
     * @param index the line index
     * @param line  the line text
     */
    void setLine(int index, @NotNull String line);

    /**
     * Adds a line to the bottom.
     *
     * @param line the line to add
     */
    void addLine(@NotNull String line);

    /**
     * Inserts a line at an index.
     *
     * @param index the index
     * @param line  the line
     */
    void insertLine(int index, @NotNull String line);

    /**
     * Removes a line.
     *
     * @param index the line index
     */
    void removeLine(int index);

    /**
     * Clears all lines.
     */
    void clearLines();

    /**
     * Gets the line spacing.
     *
     * @return spacing in blocks
     */
    double lineSpacing();

    /**
     * Sets the line spacing.
     *
     * @param spacing spacing in blocks
     */
    void setLineSpacing(double spacing);

    /**
     * Spawns this hologram for a viewer.
     *
     * @param viewer the viewer
     */
    void spawn(@NotNull Viewer viewer);

    /**
     * Despawns this hologram from a viewer.
     *
     * @param viewer the viewer
     */
    void despawn(@NotNull Viewer viewer);

    /**
     * Despawns this hologram from all viewers.
     */
    void despawn();

    /**
     * Destroys this hologram.
     */
    void destroy();

    /**
     * Gets all viewers seeing this hologram.
     *
     * @return collection of viewers
     */
    @NotNull Collection<Viewer> viewers();

    /**
     * Checks if a viewer can see this hologram.
     *
     * @param viewer the viewer
     * @return true if visible
     */
    boolean isVisibleTo(@NotNull Viewer viewer);

    /**
     * Updates the hologram for all viewers.
     */
    void update();
}
