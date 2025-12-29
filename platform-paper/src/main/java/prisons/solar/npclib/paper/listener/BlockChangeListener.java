package prisons.solar.npclib.paper.listener;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import prisons.solar.npclib.api.world.BlockPosition;
import prisons.solar.npclib.core.physics.CollisionManager;

/**
 * Listens for block changes and invalidates the collision cache.
 * This ensures NPCs react to block place/break events in real-time.
 */
public class BlockChangeListener implements Listener {

    private final CollisionManager collisionManager;

    public BlockChangeListener(CollisionManager collisionManager) {
        this.collisionManager = collisionManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        BlockPosition pos = BlockPosition.of(
                block.getWorld().getUID().toString(),
                block.getX(),
                block.getY(),
                block.getZ()
        );

        collisionManager.invalidateBlockCache(pos);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        BlockPosition pos = BlockPosition.of(
                block.getWorld().getUID().toString(),
                block.getX(),
                block.getY(),
                block.getZ()
        );

        collisionManager.invalidateBlockCache(pos);
    }
}
