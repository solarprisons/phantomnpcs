package prisons.solar.npclib.api.world;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.Position;

/**
 * Represents a block position in a world with integer coordinates.
 * This is a platform-agnostic block location representation.
 *
 * @param worldId unique identifier for the world
 * @param x       block x coordinate
 * @param y       block y coordinate
 * @param z       block z coordinate
 */
public record BlockPosition(
        @NotNull String worldId,
        int x,
        int y,
        int z
) {
    /**
     * Creates a BlockPosition from a Position by flooring the coordinates.
     *
     * @param position the position to convert
     * @return new block position
     */
    public static BlockPosition from(@NotNull Position position) {
        return new BlockPosition(
                position.worldId(),
                position.blockX(),
                position.blockY(),
                position.blockZ()
        );
    }

    /**
     * Creates a BlockPosition from coordinates.
     *
     * @param worldId world identifier
     * @param x       block x coordinate
     * @param y       block y coordinate
     * @param z       block z coordinate
     * @return new block position
     */
    public static BlockPosition of(@NotNull String worldId, int x, int y, int z) {
        return new BlockPosition(worldId, x, y, z);
    }

    /**
     * Returns a new block position offset by the given amounts.
     *
     * @param dx x offset
     * @param dy y offset
     * @param dz z offset
     * @return new block position
     */
    public BlockPosition add(int dx, int dy, int dz) {
        return new BlockPosition(worldId, x + dx, y + dy, z + dz);
    }

    /**
     * Returns a new block position with updated coordinates.
     *
     * @param x new x coordinate
     * @param y new y coordinate
     * @param z new z coordinate
     * @return new block position
     */
    public BlockPosition withCoordinates(int x, int y, int z) {
        return new BlockPosition(worldId, x, y, z);
    }

    /**
     * Converts this block position to a centered Position (entity position).
     * The resulting position will be at the center of the block (x+0.5, y, z+0.5).
     *
     * @return new position at the center of this block
     */
    public Position toPosition() {
        return Position.of(worldId, x + 0.5, y, z + 0.5);
    }

    /**
     * Calculates the distance squared to another block position.
     * Returns {@link Integer#MAX_VALUE} if worlds differ.
     *
     * @param other the other block position
     * @return distance squared, or MAX_VALUE if different worlds
     */
    public int distanceSquared(@NotNull BlockPosition other) {
        if (!worldId.equals(other.worldId)) {
            return Integer.MAX_VALUE;
        }
        int dx = x - other.x;
        int dy = y - other.y;
        int dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculates the Manhattan distance to another block position.
     * Returns {@link Integer#MAX_VALUE} if worlds differ.
     *
     * @param other the other block position
     * @return Manhattan distance, or MAX_VALUE if different worlds
     */
    public int manhattanDistance(@NotNull BlockPosition other) {
        if (!worldId.equals(other.worldId)) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z);
    }

    /**
     * Checks if this block position is in the same world as another.
     *
     * @param other the other block position
     * @return true if in the same world
     */
    public boolean isSameWorld(@NotNull BlockPosition other) {
        return worldId.equals(other.worldId);
    }
}