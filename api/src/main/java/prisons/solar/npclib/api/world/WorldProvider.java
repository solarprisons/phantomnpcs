package prisons.solar.npclib.api.world;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Platform-agnostic interface for querying world data.
 * Implementations should be provided by platform-specific modules.
 */
public interface WorldProvider {

    /**
     * Gets all block positions within the given bounding box.
     * This method should return all blocks that intersect with the AABB,
     * including partial intersections.
     *
     * @param worldId the world identifier
     * @param minX    minimum x coordinate
     * @param minY    minimum y coordinate
     * @param minZ    minimum z coordinate
     * @param maxX    maximum x coordinate
     * @param maxY    maximum y coordinate
     * @param maxZ    maximum z coordinate
     * @return collection of block positions in the AABB
     */
    @NotNull
    Collection<BlockPosition> getBlocksInAABB(
            @NotNull String worldId,
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    );

    /**
     * Checks if a block at the given position is solid (has collision).
     *
     * @param position the block position
     * @return true if the block is solid
     */
    boolean isBlockSolid(@NotNull BlockPosition position);

    /**
     * Gets the bounding box of a block at the given position.
     * For standard blocks, this is typically a 1x1x1 cube.
     * For non-full blocks (slabs, stairs, etc.), this returns the actual collision shape.
     *
     * @param position the block position
     * @return the bounding box of the block, or null if the block has no collision
     */
    BlockBoundingBox getBlockBounds(@NotNull BlockPosition position);

    /**
     * Represents a block's bounding box with optional collision data.
     */
    record BlockBoundingBox(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ,
            boolean solid
    ) {
        /**
         * Creates a standard full-block bounding box.
         *
         * @param position the block position
         * @param solid    whether the block is solid
         * @return new block bounding box
         */
        public static BlockBoundingBox full(@NotNull BlockPosition position, boolean solid) {
            return new BlockBoundingBox(
                    position.x(), position.y(), position.z(),
                    position.x() + 1.0, position.y() + 1.0, position.z() + 1.0,
                    solid
            );
        }

        /**
         * Creates a custom bounding box relative to the block position.
         *
         * @param position the block position
         * @param minX     relative minimum x (0-1)
         * @param minY     relative minimum y (0-1)
         * @param minZ     relative minimum z (0-1)
         * @param maxX     relative maximum x (0-1)
         * @param maxY     relative maximum y (0-1)
         * @param maxZ     relative maximum z (0-1)
         * @param solid    whether the block is solid
         * @return new block bounding box
         */
        public static BlockBoundingBox relative(
                @NotNull BlockPosition position,
                double minX, double minY, double minZ,
                double maxX, double maxY, double maxZ,
                boolean solid
        ) {
            return new BlockBoundingBox(
                    position.x() + minX, position.y() + minY, position.z() + minZ,
                    position.x() + maxX, position.y() + maxY, position.z() + maxZ,
                    solid
            );
        }
    }
}