package prisons.solar.npclib.core.engine;

import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Generic object pool for reducing garbage collection pressure.
 * Thread-safe implementation using lock-free queue.
 *
 * @param <T> the type of pooled objects
 */
public class ObjectPool<T> {

    private final Queue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final Consumer<T> resetter;
    private final int maxSize;

    private int created = 0;
    private int reused = 0;

    /**
     * Creates a new object pool.
     *
     * @param factory  function to create new instances
     * @param resetter function to reset objects before reuse (can be null)
     * @param maxSize  maximum pool size (excess objects are discarded)
     */
    public ObjectPool(@NotNull Supplier<T> factory, Consumer<T> resetter, int maxSize) {
        this.factory = factory;
        this.resetter = resetter;
        this.maxSize = maxSize;
    }

    /**
     * Creates a pool with default max size of 256.
     *
     * @param factory  function to create new instances
     * @param resetter function to reset objects
     */
    public ObjectPool(@NotNull Supplier<T> factory, Consumer<T> resetter) {
        this(factory, resetter, 256);
    }

    /**
     * Creates a pool without reset function.
     *
     * @param factory function to create new instances
     * @param maxSize maximum pool size
     */
    public ObjectPool(@NotNull Supplier<T> factory, int maxSize) {
        this(factory, null, maxSize);
    }

    /**
     * Acquires an object from the pool, or creates a new one if empty.
     *
     * @return the object
     */
    @NotNull
    public T acquire() {
        T obj = pool.poll();
        if (obj == null) {
            created++;
            return factory.get();
        }
        reused++;
        return obj;
    }

    /**
     * Releases an object back to the pool.
     * If the pool is full, the object is discarded.
     *
     * @param obj the object to release
     */
    public void release(@NotNull T obj) {
        if (pool.size() >= maxSize) {
            return; // Discard - pool is full
        }

        if (resetter != null) {
            resetter.accept(obj);
        }

        pool.offer(obj);
    }

    /**
     * Pre-populates the pool with objects.
     *
     * @param count number of objects to create
     */
    public void prewarm(int count) {
        for (int i = 0; i < Math.min(count, maxSize); i++) {
            pool.offer(factory.get());
            created++;
        }
    }

    /**
     * Clears the pool.
     */
    public void clear() {
        pool.clear();
    }

    /**
     * Gets the current pool size.
     *
     * @return number of available objects
     */
    public int size() {
        return pool.size();
    }

    /**
     * Gets the number of objects created.
     *
     * @return created count
     */
    public int createdCount() {
        return created;
    }

    /**
     * Gets the number of objects reused.
     *
     * @return reused count
     */
    public int reusedCount() {
        return reused;
    }

    /**
     * Gets the pool efficiency (reuse rate).
     *
     * @return percentage of acquires that were reused (0.0-1.0)
     */
    public double efficiency() {
        int total = created + reused;
        return total > 0 ? (double) reused / total : 0.0;
    }

    /**
     * Resets statistics.
     */
    public void resetStats() {
        created = 0;
        reused = 0;
    }
}
