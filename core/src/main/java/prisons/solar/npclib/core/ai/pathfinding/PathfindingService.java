package prisons.solar.npclib.core.ai.pathfinding;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.ai.pathfinding.*;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.world.BlockPosition;
import prisons.solar.npclib.api.world.WorldProvider;
import prisons.solar.npclib.core.physics.CollisionManager;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Production-ready pathfinding service with thread pooling, caching, and request budgeting
 * for handling 1000+ concurrent NPC pathfinding requests at 20 TPS.
 *
 * <p>Uses A* pathfinding with 8-directional + vertical neighbor exploration.
 * Default pathfinder can be overridden via constructor.
 */
public class PathfindingService {
    private final PathfindingComponent pathfinder;
    private final ExecutorService executor;
    private final LoadingCache<PathCacheKey, Path> pathCache;

    private final AtomicLong totalComputeTimeThisTick = new AtomicLong(0);
    private static final long MAX_COMPUTE_TIME_PER_TICK_MS = 50;

    public PathfindingService(WorldProvider worldProvider, int threadPoolSize) {
        this(worldProvider, threadPoolSize, null);
    }

    public PathfindingService(WorldProvider worldProvider, int threadPoolSize,
                              @Nullable CollisionManager collisionManager) {
        AtomicInteger threadCounter = new AtomicInteger(0);
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("phantom-pathfinding-" + threadCounter.getAndIncrement());
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            thread.setDaemon(true);
            return thread;
        };

        this.executor = new ThreadPoolExecutor(
            threadPoolSize,
            threadPoolSize * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        this.pathfinder = new AStarPathfinder(worldProvider, executor, collisionManager);

        this.pathCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build(this::computePath);
    }

    /**
     * Alternative constructor for custom pathfinder implementations.
     *
     * @param customPathfinder custom pathfinding implementation
     * @param threadPoolSize number of threads for async pathfinding
     */
    public PathfindingService(PathfindingComponent customPathfinder, int threadPoolSize) {
        AtomicInteger threadCounter = new AtomicInteger(0);
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("phantom-pathfinding-" + threadCounter.getAndIncrement());
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            thread.setDaemon(true);
            return thread;
        };

        this.executor = new ThreadPoolExecutor(
            threadPoolSize,
            threadPoolSize * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(500),
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        this.pathfinder = customPathfinder;

        this.pathCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build(this::computePath);
    }

    public CompletableFuture<PathResult> findPath(PathRequest request) {
        if (totalComputeTimeThisTick.get() > MAX_COMPUTE_TIME_PER_TICK_MS) {
            return CompletableFuture.completedFuture(PathResult.timeout(0, 0));
        }

        PathCacheKey key = createCacheKey(request);
        Path cachedPath = pathCache.getIfPresent(key);
        if (cachedPath != null) {
            return CompletableFuture.completedFuture(PathResult.success(cachedPath, 0, 0));
        }

        long startTime = System.currentTimeMillis();
        return pathfinder.findPath(request).thenApply(result -> {
            long elapsed = System.currentTimeMillis() - startTime;
            totalComputeTimeThisTick.addAndGet(elapsed);

            if (result.status() == PathResult.Status.SUCCESS) {
                pathCache.put(key, result.path());
            }

            return result;
        });
    }

    public PathfindingComponent getPathfinder() {
        return pathfinder;
    }

    /**
     * Invalidates pathfinding cache around a block position.
     * Call this when blocks are placed/broken to update pathfinding data.
     *
     * @param position block position that changed
     */
    public void invalidateRegion(Position position) {
        pathCache.invalidateAll();
    }

    public void onTick() {
        totalComputeTimeThisTick.set(0);
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private PathCacheKey createCacheKey(PathRequest request) {
        BlockPosition start = BlockPosition.from(request.start());
        BlockPosition goal = BlockPosition.from(request.goal());
        return new PathCacheKey(start, goal, request.capabilities());
    }

    private Path computePath(PathCacheKey key) {
        PathRequest request = PathRequest.builder(
            key.start().toPosition(),
            key.goal().toPosition()
        ).capabilities(key.capabilities()).build();

        PathResult result = pathfinder.findPathSync(request);
        return result.status() == PathResult.Status.SUCCESS ? result.path() : null;
    }

    record PathCacheKey(BlockPosition start, BlockPosition goal, MovementCapabilities capabilities) {}
}
