package prisons.solar.npclib.core.pool;

/**
 * Centralized manager for all object pools used in the physics engine.
 * Provides unified access to pool metrics and lifecycle management.
 *
 * <p>This class coordinates the thread-local pools and provides
 * aggregate statistics for monitoring and optimization.
 */
public class ObjectPoolManager {
    private static final ObjectPoolManager INSTANCE = new ObjectPoolManager();

    private ObjectPoolManager() {
    }

    /**
     * Gets the singleton instance.
     *
     * @return the pool manager
     */
    public static ObjectPoolManager getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the Vector3d pool for the current thread.
     *
     * @return the thread-local Vector3d pool
     */
    public Vector3dPool getVector3dPool() {
        return Vector3dPool.get();
    }

    /**
     * Gets the AABB pool for the current thread.
     *
     * @return the thread-local AABB pool
     */
    public AABBPool getAABBPool() {
        return AABBPool.get();
    }

    /**
     * Resets all pools for the current thread.
     * Call this at the end of each physics tick.
     */
    public void resetAll() {
        getVector3dPool().reset();
        getAABBPool().reset();
    }

    /**
     * Resets all metrics across all pools for the current thread.
     */
    public void resetAllMetrics() {
        getVector3dPool().resetMetrics();
        getAABBPool().resetMetrics();
    }

    /**
     * Gets aggregate pool statistics for the current thread.
     *
     * @return pool statistics
     */
    public PoolStatistics getStatistics() {
        Vector3dPool vectorPool = getVector3dPool();
        AABBPool aabbPool = getAABBPool();

        return new PoolStatistics(
                vectorPool.getTotalBorrows() + aabbPool.getTotalBorrows(),
                vectorPool.getPoolExhaustions() + aabbPool.getPoolExhaustions(),
                calculateOverallHitRate(vectorPool, aabbPool),
                calculateOverallUtilization(vectorPool, aabbPool),
                new Vector3dPoolStats(
                        vectorPool.getCapacity(),
                        vectorPool.getBorrowedCount(),
                        vectorPool.getTotalBorrows(),
                        vectorPool.getPoolExhaustions(),
                        vectorPool.getHitRate(),
                        vectorPool.getUtilization()
                ),
                new AABBPoolStats(
                        aabbPool.getCapacity(),
                        aabbPool.getBorrowedCount(),
                        aabbPool.getTotalBorrows(),
                        aabbPool.getPoolExhaustions(),
                        aabbPool.getHitRate(),
                        aabbPool.getUtilization()
                )
        );
    }

    private double calculateOverallHitRate(Vector3dPool vectorPool, AABBPool aabbPool) {
        long totalBorrows = vectorPool.getTotalBorrows() + aabbPool.getTotalBorrows();
        if (totalBorrows == 0) return 1.0;

        long totalExhaustions = vectorPool.getPoolExhaustions() + aabbPool.getPoolExhaustions();
        return 1.0 - ((double) totalExhaustions / totalBorrows);
    }

    private double calculateOverallUtilization(Vector3dPool vectorPool, AABBPool aabbPool) {
        int totalCapacity = vectorPool.getCapacity() + aabbPool.getCapacity();
        int totalBorrowed = vectorPool.getBorrowedCount() + aabbPool.getBorrowedCount();
        return (double) totalBorrowed / totalCapacity;
    }

    /**
     * Aggregate pool statistics.
     */
    public record PoolStatistics(
            long totalBorrows,
            long totalExhaustions,
            double overallHitRate,
            double overallUtilization,
            Vector3dPoolStats vector3dPool,
            AABBPoolStats aabbPool
    ) {
        public String formatReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Object Pool Statistics ===\n");
            sb.append(String.format("Total Borrows: %,d\n", totalBorrows));
            sb.append(String.format("Total Exhaustions: %,d\n", totalExhaustions));
            sb.append(String.format("Overall Hit Rate: %.1f%%\n", overallHitRate * 100));
            sb.append(String.format("Overall Utilization: %.1f%%\n", overallUtilization * 100));
            sb.append("\n");
            sb.append("Vector3d Pool:\n");
            sb.append(vector3dPool.formatReport());
            sb.append("\n");
            sb.append("AABB Pool:\n");
            sb.append(aabbPool.formatReport());
            return sb.toString();
        }
    }

    /**
     * Vector3d pool statistics.
     */
    public record Vector3dPoolStats(
            int capacity,
            int borrowed,
            long totalBorrows,
            long exhaustions,
            double hitRate,
            double utilization
    ) {
        public String formatReport() {
            return String.format(
                    "  Capacity: %,d | Borrowed: %,d | Total Borrows: %,d\n" +
                    "  Exhaustions: %,d | Hit Rate: %.1f%% | Utilization: %.1f%%",
                    capacity, borrowed, totalBorrows, exhaustions, hitRate * 100, utilization * 100
            );
        }
    }

    /**
     * AABB pool statistics.
     */
    public record AABBPoolStats(
            int capacity,
            int borrowed,
            long totalBorrows,
            long exhaustions,
            double hitRate,
            double utilization
    ) {
        public String formatReport() {
            return String.format(
                    "  Capacity: %,d | Borrowed: %,d | Total Borrows: %,d\n" +
                    "  Exhaustions: %,d | Hit Rate: %.1f%% | Utilization: %.1f%%",
                    capacity, borrowed, totalBorrows, exhaustions, hitRate * 100, utilization * 100
            );
        }
    }
}
