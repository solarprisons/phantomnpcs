package prisons.solar.npclib.api.math;

import java.util.Objects;

/**
 * Immutable 3D vector with double precision.
 * Used for positions, velocities, directions, and other spatial calculations.
 */
public class Vector3d {

    public static final Vector3d ZERO = new Vector3d(0, 0, 0);
    public static final Vector3d ONE = new Vector3d(1, 1, 1);
    public static final Vector3d UP = new Vector3d(0, 1, 0);
    public static final Vector3d DOWN = new Vector3d(0, -1, 0);
    public static final Vector3d NORTH = new Vector3d(0, 0, -1);
    public static final Vector3d SOUTH = new Vector3d(0, 0, 1);
    public static final Vector3d EAST = new Vector3d(1, 0, 0);
    public static final Vector3d WEST = new Vector3d(-1, 0, 0);

    private final double x;
    private final double y;
    private final double z;

    public Vector3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public int getBlockX() {
        return (int) Math.floor(x);
    }

    public int getBlockY() {
        return (int) Math.floor(y);
    }

    public int getBlockZ() {
        return (int) Math.floor(z);
    }

    public Vector3d add(Vector3d other) {
        return new Vector3d(x + other.x, y + other.y, z + other.z);
    }

    public Vector3d add(double x, double y, double z) {
        return new Vector3d(this.x + x, this.y + y, this.z + z);
    }

    public Vector3d subtract(Vector3d other) {
        return new Vector3d(x - other.x, y - other.y, z - other.z);
    }

    public Vector3d subtract(double x, double y, double z) {
        return new Vector3d(this.x - x, this.y - y, this.z - z);
    }

    public Vector3d multiply(double scalar) {
        return new Vector3d(x * scalar, y * scalar, z * scalar);
    }

    public Vector3d multiply(Vector3d other) {
        return new Vector3d(x * other.x, y * other.y, z * other.z);
    }

    public Vector3d divide(double scalar) {
        if (scalar == 0) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return new Vector3d(x / scalar, y / scalar, z / scalar);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSquared() {
        return x * x + y * y + z * z;
    }

    public double distance(Vector3d other) {
        return subtract(other).length();
    }

    public double distanceSquared(Vector3d other) {
        return subtract(other).lengthSquared();
    }

    public Vector3d normalize() {
        double len = length();
        if (len == 0) return ZERO;
        return divide(len);
    }

    public double dot(Vector3d other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public Vector3d cross(Vector3d other) {
        return new Vector3d(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
        );
    }

    public Vector3d negate() {
        return new Vector3d(-x, -y, -z);
    }

    public Vector3d abs() {
        return new Vector3d(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    public Vector3d floor() {
        return new Vector3d(Math.floor(x), Math.floor(y), Math.floor(z));
    }

    public Vector3d ceil() {
        return new Vector3d(Math.ceil(x), Math.ceil(y), Math.ceil(z));
    }

    public Vector3d round() {
        return new Vector3d(Math.round(x), Math.round(y), Math.round(z));
    }

    public Vector3d min(Vector3d other) {
        return new Vector3d(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
    }

    public Vector3d max(Vector3d other) {
        return new Vector3d(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
    }

    public Vector3d lerp(Vector3d other, double t) {
        return new Vector3d(
                x + (other.x - x) * t,
                y + (other.y - y) * t,
                z + (other.z - z) * t
        );
    }

    public double angle(Vector3d other) {
        double dot = dot(other);
        double lenProduct = length() * other.length();
        if (lenProduct == 0) return 0;
        return Math.acos(Math.min(1, Math.max(-1, dot / lenProduct)));
    }

    public Vector3d midpoint(Vector3d other) {
        return new Vector3d((x + other.x) / 2, (y + other.y) / 2, (z + other.z) / 2);
    }

    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vector3d vector3d = (Vector3d) o;
        return Double.compare(vector3d.x, x) == 0 &&
                Double.compare(vector3d.y, y) == 0 &&
                Double.compare(vector3d.z, z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "Vector3d(" + x + ", " + y + ", " + z + ")";
    }
}
