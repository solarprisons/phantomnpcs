package prisons.solar.npclib.api.math;

import org.jetbrains.annotations.NotNull;

/**
 * Mutable version of Vector3d for use in object pooling.
 * This class is designed for internal use in hot paths to reduce allocation.
 *
 * <p><b>WARNING:</b> This class is NOT thread-safe. Do not share instances across threads.
 * Use {@link Vector3d} for public APIs and thread-safe operations.
 *
 * @see Vector3d
 */
public class MutableVector3d {
    private double x;
    private double y;
    private double z;

    /**
     * Creates a mutable vector initialized to (0, 0, 0).
     */
    public MutableVector3d() {
        this(0, 0, 0);
    }

    /**
     * Creates a mutable vector with the specified components.
     *
     * @param x the x component
     * @param y the y component
     * @param z the z component
     */
    public MutableVector3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Creates a mutable vector from an immutable vector.
     *
     * @param vector the vector to copy
     */
    public MutableVector3d(@NotNull Vector3d vector) {
        this(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Sets all components of this vector.
     *
     * @param x the new x component
     * @param y the new y component
     * @param z the new z component
     * @return this vector for chaining
     */
    public MutableVector3d set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /**
     * Sets this vector to match another vector.
     *
     * @param other the vector to copy
     * @return this vector for chaining
     */
    public MutableVector3d set(@NotNull Vector3d other) {
        this.x = other.getX();
        this.y = other.getY();
        this.z = other.getZ();
        return this;
    }

    /**
     * Sets this vector to match another mutable vector.
     *
     * @param other the vector to copy
     * @return this vector for chaining
     */
    public MutableVector3d set(@NotNull MutableVector3d other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        return this;
    }

    /**
     * Adds another vector to this vector in-place.
     *
     * @param x the x component to add
     * @param y the y component to add
     * @param z the z component to add
     * @return this vector for chaining
     */
    public MutableVector3d add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    /**
     * Adds another vector to this vector in-place.
     *
     * @param other the vector to add
     * @return this vector for chaining
     */
    public MutableVector3d add(@NotNull Vector3d other) {
        this.x += other.getX();
        this.y += other.getY();
        this.z += other.getZ();
        return this;
    }

    /**
     * Multiplies this vector by a scalar in-place.
     *
     * @param scalar the scalar to multiply by
     * @return this vector for chaining
     */
    public MutableVector3d multiply(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    /**
     * Multiplies each component by the corresponding component in another vector.
     *
     * @param x the x multiplier
     * @param y the y multiplier
     * @param z the z multiplier
     * @return this vector for chaining
     */
    public MutableVector3d multiply(double x, double y, double z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    /**
     * Sets this vector to zero (0, 0, 0).
     *
     * @return this vector for chaining
     */
    public MutableVector3d zero() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        return this;
    }

    /**
     * Calculates the squared length of this vector.
     * Useful for distance comparisons without the cost of sqrt.
     *
     * @return the squared length
     */
    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    /**
     * Calculates the length of this vector.
     *
     * @return the length
     */
    public double length() {
        return Math.sqrt(lengthSquared());
    }

    /**
     * Creates an immutable copy of this vector.
     *
     * @return an immutable Vector3d with the same components
     */
    public Vector3d toImmutable() {
        return new Vector3d(x, y, z);
    }

    /**
     * Checks if this vector contains valid (non-NaN, non-infinite) values.
     *
     * @return true if all components are valid
     */
    public boolean isValid() {
        return !Double.isNaN(x) && !Double.isInfinite(x) &&
               !Double.isNaN(y) && !Double.isInfinite(y) &&
               !Double.isNaN(z) && !Double.isInfinite(z);
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("MutableVector3d(%.2f, %.2f, %.2f)", x, y, z);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MutableVector3d other)) return false;
        return Double.compare(other.x, x) == 0 &&
               Double.compare(other.y, y) == 0 &&
               Double.compare(other.z, z) == 0;
    }

    @Override
    public int hashCode() {
        long bits = Double.doubleToLongBits(x);
        bits ^= Double.doubleToLongBits(y) * 31;
        bits ^= Double.doubleToLongBits(z) * 31 * 31;
        return (int) (bits ^ (bits >>> 32));
    }
}
