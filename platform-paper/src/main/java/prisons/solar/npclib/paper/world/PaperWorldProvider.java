package prisons.solar.npclib.paper.world;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.world.BlockPosition;
import prisons.solar.npclib.api.world.WorldProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Paper/Bukkit implementation of WorldProvider.
 * Provides world data queries using the Bukkit API.
 */
public class PaperWorldProvider implements WorldProvider {

    @Override
    public @NotNull Collection<BlockPosition> getBlocksInAABB(
            @NotNull String worldId,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        World world = getWorld(worldId);
        if (world == null) {
            return Collections.emptyList();
        }

        // Calculate block coordinates
        int minBlockX = (int) Math.floor(minX);
        int minBlockY = (int) Math.floor(minY);
        int minBlockZ = (int) Math.floor(minZ);
        int maxBlockX = (int) Math.floor(maxX);
        int maxBlockY = (int) Math.floor(maxY);
        int maxBlockZ = (int) Math.floor(maxZ);

        Collection<BlockPosition> blocks = new ArrayList<>();

        // Iterate through all blocks in the bounding box
        for (int x = minBlockX; x <= maxBlockX; x++) {
            for (int y = minBlockY; y <= maxBlockY; y++) {
                for (int z = minBlockZ; z <= maxBlockZ; z++) {
                    blocks.add(BlockPosition.of(worldId, x, y, z));
                }
            }
        }

        return blocks;
    }

    @Override
    public boolean isBlockSolid(@NotNull BlockPosition position) {
        World world = getWorld(position.worldId());
        if (world == null) {
            return false;
        }

        Block block = world.getBlockAt(position.x(), position.y(), position.z());
        Material type = block.getType();

        if (type.isAir()) {
            return false;
        }

        return type.isSolid() || type.isOccluding();
    }

    @Override
    public BlockBoundingBox getBlockBounds(@NotNull BlockPosition position) {
        World world = getWorld(position.worldId());
        if (world == null) {
            return null;  // Only return null if world doesn't exist
        }

        Block block = world.getBlockAt(position.x(), position.y(), position.z());
        Material type = block.getType();

        // AIR blocks should return non-solid bounds, not null
        // Pathfinding needs to know AIR is walkable (exists but not solid)
        if (type.isAir()) {
            return BlockBoundingBox.full(position, false);
        }

        // Non-solid blocks (grass, flowers, etc.) are also walkable
        if (!type.isSolid()) {
            return BlockBoundingBox.full(position, false);
        }

        // Solid blocks - get actual bounding box
        BoundingBox bukkitBounds = block.getBoundingBox();
        if (bukkitBounds == null) {
            // Shouldn't happen for solid blocks, but handle gracefully
            return BlockBoundingBox.full(position, true);
        }

        // Convert Bukkit BoundingBox to our BlockBoundingBox
        return new BlockBoundingBox(
                bukkitBounds.getMinX(),
                bukkitBounds.getMinY(),
                bukkitBounds.getMinZ(),
                bukkitBounds.getMaxX(),
                bukkitBounds.getMaxY(),
                bukkitBounds.getMaxZ(),
                true
        );
    }

    /**
     * Gets a Bukkit world by its UUID.
     *
     * @param worldId the world UUID as a string
     * @return the world, or null if not found
     */
    private World getWorld(@NotNull String worldId) {
        UUID uuid = UUID.fromString(worldId);
        World world = Bukkit.getWorld(uuid);

        if (world == null) {
            System.err.println(String.format(
                    "[PhantomNPC] WARNING: World not found for UUID %s. " +
                    "World may be unloaded or the NPC was created in a different server instance.",
                    worldId
            ));
        }

        return world;
    }
}
