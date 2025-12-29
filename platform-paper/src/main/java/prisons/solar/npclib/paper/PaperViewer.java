package prisons.solar.npclib.paper;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.viewer.Viewer;

import java.util.UUID;

/**
 * Paper implementation of {@link Viewer}.
 */
public class PaperViewer implements Viewer {

    private final Player player;

    public PaperViewer(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public @NotNull UUID id() {
        return player.getUniqueId();
    }

    @Override
    public @NotNull Position position() {
        var loc = player.getLocation();
        return Position.of(
                loc.getWorld().getUID().toString(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch()
        );
    }

    @Override
    public @NotNull String name() {
        return player.getName();
    }

    @Override
    public boolean isOnline() {
        return player.isOnline();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T platformPlayer() {
        return (T) player;
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return player.hasPermission(permission);
    }

    /**
     * Gets the underlying Bukkit player.
     *
     * @return the player
     */
    public @NotNull Player getPlayer() {
        return player;
    }
}
