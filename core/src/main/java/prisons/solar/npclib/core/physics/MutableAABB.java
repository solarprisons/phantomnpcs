package prisons.solar.npclib.core.physics;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.math.MutableVector3d;
import prisons.solar.npclib.api.math.Vector3d;
import prisons.solar.npclib.api.npc.Position;

/**
 * Mutable version of AABB for use in object pooling.
 * This class is designed for internal use in hot paths to reduce allocation.
 *
 * <p><b>WARNING:</b> This class is NOT thread-safe. Do not share instances across threads.
 * Use {@link AABB} for public APIs and thread-safe operations.
 *
 * @see AABB
 */
public class MutableAABB {
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;

    /**
     * Creates a mutable AABB initialized to zero.
     */
    public MutableAABB() {
        this(0, 0, 0, 0, 0, 0);
    }

    /**
     * Creates a mutable AABB with the specified bounds.
     *
     * @param minX minimum X coordinate
     * @param minY minimum Y coordinate
     * @param minZ minimum Z coordinate
     * @param maxX maximum X coordinate
     * @param maxY maximum Y coordinate
     * @param maxZ maximum Z coordinate
     */
    public MutableAABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /**
     * Creates a mutable AABB from an immutable AABB.
     *
     * @param aabb the AABB to copy
     */
    public MutableAABB(@NotNull AABB aabb) {
        this(aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ());
    }

    /**
     * Sets all bounds of this AABB.
     *
     * @param minX minimum X coordinate
     * @param minY minimum Y coordinate
     * @param minZ minimum Z coordinate
     * @param maxX maximum X coordinate
     * @param maxY maximum Y coordinate
     * @param maxZ maximum Z coordinate
     * @return this AABB for chaining
     */
    public MutableAABB set(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        return this;
    }

    /**
     * Sets this AABB to match another AABB.
     *
     * @param other the AABB to copy
     * @return this AABB for chaining
     */
    public MutableAABB set(@NotNull AABB other) {
        return set(other.minX(), other.minY(), other.minZ(), other.maxX(), other.maxY(), other.maxZ());
    }

    /**
     * Sets this AABB to match another mutable AABB.
     *
     * @param other the AABB to copy
     * @return this AABB for chaining
     */
    public MutableAABB set(@NotNull MutableAABB other) {
        return set(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
    }

    /**
     * Sets this AABB from an entity type and position.
     * This is the mutable equivalent of {@link AABB#fromEntityType(EntityType, Position)}.
     *
     * @param type the entity type
     * @param pos the entity position
     * @return this AABB for chaining
     */
    public MutableAABB setFromEntity(@NotNull EntityType type, @NotNull Position pos) {
        return switch (type) {
            case PLAYER -> set(
                    pos.x() - 0.3, pos.y(), pos.z() - 0.3,
                    pos.x() + 0.3, pos.y() + 1.8, pos.z() + 0.3
            );
            case ZOMBIE, SKELETON -> set(
                    pos.x() - 0.3, pos.y(), pos.z() - 0.3,
                    pos.x() + 0.3, pos.y() + 1.95, pos.z() + 0.3
            );
            default -> set(
                    pos.x() - 0.5, pos.y(), pos.z() - 0.5,
                    pos.x() + 0.5, pos.y() + 1.0, pos.z() + 0.5
            );
        };
    }

    /**
     * Offsets this AABB by the given vector in-place.
     *
     * @param offsetX the X offset
     * @param offsetY the Y offset
     * @param offsetZ the Z offset
     * @return this AABB for chaining
     */
    public MutableAABB offset(double offsetX, double offsetY, double offsetZ) {
        this.minX += offsetX;
        this.minY += offsetY;
        this.minZ += offsetZ;
        this.maxX += offsetX;
        this.maxY += offsetY;
        this.maxZ += offsetZ;
        return this;
    }

    /**
     * Offsets this AABB by the given vector in-place.
     *
     * @param offset the offset vector
     * @return this AABB for chaining
     */
    public MutableAABB offset(@NotNull Vector3d offset) {
        return offset(offset.getX(), offset.getY(), offset.getZ());
    }

    /**
     * Offsets this AABB by the given mutable vector in-place.
     *
     * @param offset the offset vector
     * @return this AABB for chaining
     */
    public MutableAABB offset(@NotNull MutableVector3d offset) {
        return offset(offset.getX(), offset.getY(), offset.getZ());
    }

    /**
     * Sets this AABB to another AABB offset by a vector.
     * Avoids creating an intermediate AABB.
     *
     * @param base the base AABB
     * @param offsetX the X offset
     * @param offsetY the Y offset
     * @param offsetZ the Z offset
     * @return this AABB for chaining
     */
    public MutableAABB setOffset(@NotNull AABB base, double offsetX, double offsetY, double offsetZ) {
        this.minX = base.minX() + offsetX;
        this.minY = base.minY() + offsetY;
        this.minZ = base.minZ() + offsetZ;
        this.maxX = base.maxX() + offsetX;
        this.maxY = base.maxY() + offsetY;
        this.maxZ = base.maxZ() + offsetZ;
        return this;
    }

    /**
     * Sets this AABB to another AABB offset by a vector.
     *
     * @param base the base AABB
     * @param offset the offset vector
     * @return this AABB for chaining
     */
    public MutableAABB setOffset(@NotNull AABB base, @NotNull Vector3d offset) {
        return setOffset(base, offset.getX(), offset.getY(), offset.getZ());
    }

    /**
     * Checks if this AABB intersects with another AABB.
     *
     * @param other the other AABB
     * @return true if the AABBs intersect
     */
    public boolean intersects(@NotNull AABB other) {
        return minX < other.maxX() && maxX > other.minX() &&
               minY < other.maxY() && maxY > other.minY() &&
               minZ < other.maxZ() && maxZ > other.minZ();
    }

    /**
     * Checks if this AABB intersects with another mutable AABB.
     *
     * @param other the other AABB
     * @return true if the AABBs intersect
     */
    public boolean intersects(@NotNull MutableAABB other) {
        return minX < other.maxX && maxX > other.minX &&
               minY < other.maxY && maxY > other.minY &&
               minZ < other.maxZ && maxZ > other.minZ;
    }

    /**
     * Calculates the center point of this AABB and stores it in the given vector.
     *
     * @param result the vector to store the result in
     */
    public void center(@NotNull MutableVector3d result) {
        result.set(
                (minX + maxX) / 2,
                (minY + maxY) / 2,
                (minZ + maxZ) / 2
        );
    }

    /**
     * Creates an immutable copy of this AABB.
     *
     * @return an immutable AABB with the same bounds
     */
    public AABB toImmutable() {
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    // Getters
    public double minX() {
        return minX;
    }

    public double minY() {
        return minY;
    }

    public double minZ() {
        return minZ;
    }

    public double maxX() {
        return maxX;
    }

    public double maxY() {
        return maxY;
    }

    public double maxZ() {
        return maxZ;
    }

    @Override
    public String toString() {
        return String.format("MutableAABB[min=(%.2f, %.2f, %.2f), max=(%.2f, %.2f, %.2f)]",
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MutableAABB other)) return false;
        return Double.compare(other.minX, minX) == 0 &&
               Double.compare(other.minY, minY) == 0 &&
               Double.compare(other.minZ, minZ) == 0 &&
               Double.compare(other.maxX, maxX) == 0 &&
               Double.compare(other.maxY, maxY) == 0 &&
               Double.compare(other.maxZ, maxZ) == 0;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(minX);
        bits ^= Double.doubleToLongBits(minY) * 31;
        bits ^= Double.doubleToLongBits(minZ) * 31 * 31;
        bits ^= Double.doubleToLongBits(maxX) * 31 * 31 * 31;
        bits ^= Double.doubleToLongBits(maxY) * 31 * 31 * 31 * 31;
        bits ^= Double.doubleToLongBits(maxZ) * 31 * 31 * 31 * 31 * 31;
        return Long.hashCode(bits);
    }
}
