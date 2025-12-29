package prisons.solar.npclib.core.pool;

import prisons.solar.npclib.api.math.MutableVector3d;
import prisons.solar.npclib.api.math.Vector3d;

/**
 * Thread-local object pool for {@link MutableVector3d} instances.
 * Designed for use in single-threaded hot paths (e.g., physics tick loop).
 *
 * <p>Usage pattern:
 * <pre>{@code
 * Vector3dPool pool = Vector3dPool.get();
 * MutableVector3d v = pool.borrow(1, 2, 3);
 * // ... use vector ...
 * pool.reset(); // At end of tick
 * }</pre>
 */
public class Vector3dPool {
    private static final ThreadLocal<Vector3dPool> INSTANCE = ThreadLocal.withInitial(Vector3dPool::new);

    private final MutableVector3d[] pool;
    private int index = 0;
    private long totalBorrows = 0;
    private long poolExhaustions = 0;

    /**
     * Default pool size (10,000 vectors).
     */
    private static final int DEFAULT_SIZE = 10_000;

    /**
     * Gets the thread-local pool instance.
     *
     * @return the pool for the current thread
     */
    public static Vector3dPool get() {
        return INSTANCE.get();
    }

    /**
     * Creates a pool with default size.
     */
    public Vector3dPool() {
        this(DEFAULT_SIZE);
    }

    /**
     * Creates a pool with the specified size.
     *
     * @param size the pool size
     */
    public Vector3dPool(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Pool size must be positive");
        }
        this.pool = new MutableVector3d[size];
        for (int i = 0; i < size; i++) {
            pool[i] = new MutableVector3d();
        }
    }

    /**
     * Borrows a vector from the pool, initialized with the given values.
     * If the pool is exhausted, allocates a new vector (graceful degradation).
     *
     * @param x the x component
     * @param y the y component
     * @param z the z component
     * @return a mutable vector (may be pooled or newly allocated)
     */
    public MutableVector3d borrow(double x, double y, double z) {
        totalBorrows++;

        if (index >= pool.length) {
            // Pool exhausted - allocate new instance and warn
            poolExhaustions++;
            if (poolExhaustions % 1000 == 0) {
                System.err.println(String.format(
                        "[PhantomNPC Pool] WARNING: Vector3dPool exhausted %d times (pool size: %d). " +
                        "Consider increasing pool size.",
                        poolExhaustions, pool.length
                ));
            }
            return new MutableVector3d(x, y, z);
        }

        MutableVector3d vector = pool[index++];
        vector.set(x, y, z);
        return vector;
    }

    /**
     * Borrows a vector initialized to zero.
     *
     * @return a mutable vector set to (0, 0, 0)
     */
    public MutableVector3d borrowZero() {
        return borrow(0, 0, 0);
    }

    /**
     * Borrows a vector initialized from an immutable vector.
     *
     * @param vector the vector to copy
     * @return a mutable vector with the same values
     */
    public MutableVector3d borrow(Vector3d vector) {
        return borrow(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Resets the pool, making all vectors available for reuse.
     * Call this at the end of each tick.
     */
    public void reset() {
        index = 0;
    }

    /**
     * Gets the current pool utilization (0.0 to 1.0).
     *
     * @return the fraction of the pool currently in use
     */
    public double getUtilization() {
        return (double) index / pool.length;
    }

    /**
     * Gets the total number of borrows since creation.
     *
     * @return total borrow count
     */
    public long getTotalBorrows() {
        return totalBorrows;
    }

    /**
     * Gets the number of times the pool was exhausted.
     *
     * @return exhaustion count
     */
    public long getPoolExhaustions() {
        return poolExhaustions;
    }

    /**
     * Gets the pool hit rate (percentage of borrows that used pooled objects).
     *
     * @return hit rate from 0.0 to 1.0
     */
    public double getHitRate() {
        if (totalBorrows == 0) return 1.0;
        return 1.0 - ((double) poolExhaustions / totalBorrows);
    }

    /**
     * Gets the pool capacity.
     *
     * @return the maximum number of vectors in the pool
     */
    public int getCapacity() {
        return pool.length;
    }

    /**
     * Gets the current number of borrowed vectors.
     *
     * @return number of vectors currently in use
     */
    public int getBorrowedCount() {
        return index;
    }

    /**
     * Resets all metrics.
     */
    public void resetMetrics() {
        totalBorrows = 0;
        poolExhaustions = 0;
    }
}
