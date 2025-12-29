package prisons.solar.npclib.core.pool;

import prisons.solar.npclib.api.entity.EntityType;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.core.physics.AABB;
import prisons.solar.npclib.core.physics.MutableAABB;

/**
 * Thread-local object pool for {@link MutableAABB} instances.
 * Designed for use in single-threaded hot paths (e.g., collision detection).
 *
 * <p>Usage pattern:
 * <pre>{@code
 * AABBPool pool = AABBPool.get();
 * MutableAABB aabb = pool.borrow();
 * aabb.setFromEntity(EntityType.PLAYER, position);
 * // ... use AABB ...
 * pool.reset(); // At end of tick
 * }</pre>
 */
public class AABBPool {
    private static final ThreadLocal<AABBPool> INSTANCE = ThreadLocal.withInitial(AABBPool::new);

    private final MutableAABB[] pool;
    private int index = 0;
    private long totalBorrows = 0;
    private long poolExhaustions = 0;

    /**
     * Default pool size (5,000 AABBs).
     */
    private static final int DEFAULT_SIZE = 5_000;

    /**
     * Gets the thread-local pool instance.
     *
     * @return the pool for the current thread
     */
    public static AABBPool get() {
        return INSTANCE.get();
    }

    /**
     * Creates a pool with default size.
     */
    public AABBPool() {
        this(DEFAULT_SIZE);
    }

    /**
     * Creates a pool with the specified size.
     *
     * @param size the pool size
     */
    public AABBPool(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Pool size must be positive");
        }
        this.pool = new MutableAABB[size];
        for (int i = 0; i < size; i++) {
            pool[i] = new MutableAABB();
        }
    }

    /**
     * Borrows an AABB from the pool, uninitialized.
     * Caller must initialize the AABB before use.
     *
     * @return a mutable AABB (may be pooled or newly allocated)
     */
    public MutableAABB borrow() {
        totalBorrows++;

        if (index >= pool.length) {
            // Pool exhausted - allocate new instance and warn
            poolExhaustions++;
            if (poolExhaustions % 1000 == 0) {
                System.err.printf(
                        "[PhantomNPC Pool] WARNING: AABBPool exhausted %d times (pool size: %d). " +
                                "Consider increasing pool size.%n",
                        poolExhaustions, pool.length
                );
            }
            return new MutableAABB();
        }

        return pool[index++];
    }

    /**
     * Borrows an AABB initialized from an entity type and position.
     *
     * @param type the entity type
     * @param position the entity position
     * @return a mutable AABB for the entity
     */
    public MutableAABB borrowForEntity(EntityType type, Position position) {
        MutableAABB aabb = borrow();
        return aabb.setFromEntity(type, position);
    }

    /**
     * Borrows an AABB initialized from an immutable AABB.
     *
     * @param source the AABB to copy
     * @return a mutable AABB with the same bounds
     */
    public MutableAABB borrow(AABB source) {
        MutableAABB aabb = borrow();
        aabb.set(source);
        return aabb;
    }

    /**
     * Borrows an AABB initialized with the given bounds.
     *
     * @param minX minimum X coordinate
     * @param minY minimum Y coordinate
     * @param minZ minimum Z coordinate
     * @param maxX maximum X coordinate
     * @param maxY maximum Y coordinate
     * @param maxZ maximum Z coordinate
     * @return a mutable AABB with the specified bounds
     */
    public MutableAABB borrow(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        MutableAABB aabb = borrow();
        aabb.set(minX, minY, minZ, maxX, maxY, maxZ);
        return aabb;
    }

    /**
     * Resets the pool, making all AABBs available for reuse.
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
     * @return the maximum number of AABBs in the pool
     */
    public int getCapacity() {
        return pool.length;
    }

    /**
     * Gets the current number of borrowed AABBs.
     *
     * @return number of AABBs currently in use
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
