package prisons.solar.npclib.api.viewer;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.Position;

import java.util.UUID;

/**
 * Represents a player who can view NPCs.
 * This is a platform-agnostic abstraction over player objects.
 */
public interface Viewer {

    /**
     * Gets the unique identifier of this viewer.
     *
     * @return the viewer's UUID
     */
    @NotNull UUID id();

    /**
     * Gets the current position of this viewer.
     *
     * @return the position
     */
    @NotNull Position position();

    /**
     * Gets the name of this viewer.
     *
     * @return the viewer's name
     */
    @NotNull String name();

    /**
     * Checks if this viewer is online and valid.
     *
     * @return true if online
     */
    boolean isOnline();

    /**
     * Gets the underlying platform player object.
     * The type depends on the platform (e.g., org.bukkit.entity.Player for Paper).
     *
     * @param <T> the expected player type
     * @return the platform player object
     * @throws ClassCastException if the type doesn't match
     */
    <T> T platformPlayer();

    /**
     * Checks if this viewer has a permission.
     *
     * @param permission the permission node
     * @return true if has permission
     */
    boolean hasPermission(@NotNull String permission);
}
