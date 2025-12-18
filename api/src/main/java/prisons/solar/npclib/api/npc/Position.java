package prisons.solar.npclib.api.npc;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a position in the world with rotation.
 * This is a platform-agnostic location representation.
 *
 * @param worldId unique identifier for the world
 * @param x       x coordinate
 * @param y       y coordinate
 * @param z       z coordinate
 * @param yaw     horizontal rotation (0-360)
 * @param pitch   vertical rotation (-90 to 90)
 */
public record Position(
        @NotNull String worldId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {
    /**
     * Creates a position without rotation.
     *
     * @param worldId world identifier
     * @param x       x coordinate
     * @param y       y coordinate
     * @param z       z coordinate
     * @return new position
     */
    public static Position of(@NotNull String worldId, double x, double y, double z) {
        return new Position(worldId, x, y, z, 0f, 0f);
    }

    /**
     * Creates a position with full rotation.
     *
     * @param worldId world identifier
     * @param x       x coordinate
     * @param y       y coordinate
     * @param z       z coordinate
     * @param yaw     horizontal rotation
     * @param pitch   vertical rotation
     * @return new position
     */
    public static Position of(@NotNull String worldId, double x, double y, double z, float yaw, float pitch) {
        return new Position(worldId, x, y, z, yaw, pitch);
    }

    /**
     * Returns a new position with updated coordinates.
     *
     * @param x new x coordinate
     * @param y new y coordinate
     * @param z new z coordinate
     * @return new position
     */
    public Position withCoordinates(double x, double y, double z) {
        return new Position(worldId, x, y, z, yaw, pitch);
    }

    /**
     * Returns a new position with updated rotation.
     *
     * @param yaw   new yaw
     * @param pitch new pitch
     * @return new position
     */
    public Position withRotation(float yaw, float pitch) {
        return new Position(worldId, x, y, z, yaw, pitch);
    }

    /**
     * Returns a new position offset by the given amounts.
     *
     * @param dx x offset
     * @param dy y offset
     * @param dz z offset
     * @return new position
     */
    public Position add(double dx, double dy, double dz) {
        return new Position(worldId, x + dx, y + dy, z + dz, yaw, pitch);
    }

    /**
     * Calculates the distance squared to another position.
     * Returns {@link Double#MAX_VALUE} if worlds differ.
     *
     * @param other the other position
     * @return distance squared, or MAX_VALUE if different worlds
     */
    public double distanceSquared(@NotNull Position other) {
        if (!worldId.equals(other.worldId)) {
            return Double.MAX_VALUE;
        }
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculates the distance to another position.
     * Returns {@link Double#MAX_VALUE} if worlds differ.
     *
     * @param other the other position
     * @return distance, or MAX_VALUE if different worlds
     */
    public double distance(@NotNull Position other) {
        double distSq = distanceSquared(other);
        return distSq == Double.MAX_VALUE ? Double.MAX_VALUE : Math.sqrt(distSq);
    }

    /**
     * Gets the block X coordinate.
     *
     * @return block x
     */
    public int blockX() {
        return (int) Math.floor(x);
    }

    /**
     * Gets the block Y coordinate.
     *
     * @return block y
     */
    public int blockY() {
        return (int) Math.floor(y);
    }

    /**
     * Gets the block Z coordinate.
     *
     * @return block z
     */
    public int blockZ() {
        return (int) Math.floor(z);
    }
}
