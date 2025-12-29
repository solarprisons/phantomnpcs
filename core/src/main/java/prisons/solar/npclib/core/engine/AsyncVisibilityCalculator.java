package prisons.solar.npclib.core.engine;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.NPCRegistry;
import prisons.solar.npclib.api.viewer.Viewer;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Performs visibility calculations asynchronously to avoid blocking the main thread.
 * Results are queued and processed on the main thread to ensure thread safety.
 */
public class AsyncVisibilityCalculator {

    /**
     * Result of a visibility calculation.
     */
    public record VisibilityResult(
            @NotNull Viewer viewer,
            @NotNull Set<NPC<?>> toSpawn,
            @NotNull Set<NPC<?>> toDespawn
    ) {}

    private final NPCRegistry registry;
    private final BiPredicate<NPC<?>, Viewer> visibilityCheck;
    private final BiConsumer<NPC<?>, Viewer> spawnCallback;
    private final BiConsumer<NPC<?>, Viewer> despawnCallback;
    private volatile double viewDistanceSquared;

    private final ExecutorService executor;
    private final Queue<VisibilityResult> resultQueue = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Set<UUID>> viewerVisibleNPCs = new ConcurrentHashMap<>();

    private volatile boolean running = false;

    // Statistics
    private long calculationsPerformed = 0;
    private long totalCalculationTimeNs = 0;
    private long spawnsTriggered = 0;
    private long despawnsTriggered = 0;

    /**
     * Creates a new async visibility calculator.
     *
     * @param registry        the NPC registry
     * @param viewDistance    the view distance in blocks
     * @param visibilityCheck additional visibility check (can be null)
     * @param spawnCallback   callback when an NPC should spawn for a viewer
     * @param despawnCallback callback when an NPC should despawn for a viewer
     */
    public AsyncVisibilityCalculator(
            @NotNull NPCRegistry registry,
            double viewDistance,
            BiPredicate<NPC<?>, Viewer> visibilityCheck,
            @NotNull BiConsumer<NPC<?>, Viewer> spawnCallback,
            @NotNull BiConsumer<NPC<?>, Viewer> despawnCallback
    ) {
        this.registry = registry;
        this.viewDistanceSquared = viewDistance * viewDistance;
        this.visibilityCheck = visibilityCheck;
        this.spawnCallback = spawnCallback;
        this.despawnCallback = despawnCallback;

        this.executor = Executors.newFixedThreadPool(
                Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
                r -> {
                    Thread t = new Thread(r, "Phantom-VisibilityCalc");
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    return t;
                }
        );
    }

    /**
     * Creates a calculator with default visibility check (distance only).
     */
    public AsyncVisibilityCalculator(
            @NotNull NPCRegistry registry,
            double viewDistance,
            @NotNull BiConsumer<NPC<?>, Viewer> spawnCallback,
            @NotNull BiConsumer<NPC<?>, Viewer> despawnCallback
    ) {
        this(registry, viewDistance, null, spawnCallback, despawnCallback);
    }

    /**
     * Starts the calculator.
     */
    public void start() {
        running = true;
    }

    /**
     * Stops the calculator.
     */
    public void stop() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        resultQueue.clear();
        viewerVisibleNPCs.clear();
    }

    /**
     * Submits a viewer for async visibility calculation.
     *
     * @param viewer the viewer to check
     */
    public void submitViewer(@NotNull Viewer viewer) {
        if (!running) {
            return;
        }

        executor.submit(() -> {
            try {
                calculateVisibility(viewer);
            } catch (Exception e) {
                // Swallow - don't let single viewer crash visibility thread
            }
        });
    }

    /**
     * Submits multiple viewers for async visibility calculation.
     *
     * @param viewers the viewers to check
     */
    public void submitViewers(@NotNull Collection<Viewer> viewers) {
        if (!running) {
            return;
        }

        for (Viewer viewer : viewers) {
            executor.submit(() -> calculateVisibility(viewer));
        }
    }

    /**
     * Processes pending visibility results on the main thread.
     * Call this from the main server tick.
     *
     * @param maxResults maximum results to process per call (0 for unlimited)
     * @return number of results processed
     */
    public int processResults(int maxResults) {
        int processed = 0;
        VisibilityResult result;

        while ((maxResults <= 0 || processed < maxResults) && (result = resultQueue.poll()) != null) {
            processResult(result);
            processed++;
        }

        return processed;
    }

    /**
     * Processes all pending visibility results.
     *
     * @return number of results processed
     */
    public int processAllResults() {
        return processResults(0);
    }

    private void calculateVisibility(@NotNull Viewer viewer) {
        if (!running) {
            return;
        }

        long startTime = System.nanoTime();

        Set<UUID> currentlyVisible = viewerVisibleNPCs.computeIfAbsent(
                viewer.id(), k -> ConcurrentHashMap.newKeySet()
        );

        Set<NPC<?>> toSpawn = new HashSet<>();
        Set<NPC<?>> toDespawn = new HashSet<>();
        Set<UUID> nowVisible = new HashSet<>();

        // Check all NPCs
        for (NPC<?> npc : registry.all()) {
            boolean shouldBeVisible = shouldBeVisible(npc, viewer);

            if (shouldBeVisible) {
                nowVisible.add(npc.getId());
                if (!currentlyVisible.contains(npc.getId())) {
                    toSpawn.add(npc);
                }
            } else if (currentlyVisible.contains(npc.getId())) {
                toDespawn.add(npc);
            }
        }

        // Update visible set
        currentlyVisible.clear();
        currentlyVisible.addAll(nowVisible);

        // Queue result for main thread processing
        if (!toSpawn.isEmpty() || !toDespawn.isEmpty()) {
            resultQueue.offer(new VisibilityResult(viewer, toSpawn, toDespawn));
        }

        long endTime = System.nanoTime();
        calculationsPerformed++;
        totalCalculationTimeNs += (endTime - startTime);
    }

    private boolean shouldBeVisible(@NotNull NPC<?> npc, @NotNull Viewer viewer) {
        // Check same world
        if (!npc.getPosition().worldId().equals(viewer.position().worldId())) {
            return false;
        }

        // Check distance
        double distanceSq = npc.getPosition().distanceSquared(viewer.position());
        if (distanceSq > viewDistanceSquared) {
            return false;
        }

        // Check custom visibility predicate
        if (visibilityCheck != null && !visibilityCheck.test(npc, viewer)) {
            return false;
        }

        return true;
    }

    private void processResult(@NotNull VisibilityResult result) {
        // Spawn NPCs
        for (NPC<?> npc : result.toSpawn()) {
            try {
                spawnCallback.accept(npc, result.viewer());
                spawnsTriggered++;
            } catch (Exception e) {
                // Swallow callback errors to continue processing
            }
        }

        // Despawn NPCs
        for (NPC<?> npc : result.toDespawn()) {
            try {
                despawnCallback.accept(npc, result.viewer());
                despawnsTriggered++;
            } catch (Exception e) {
                // Swallow callback errors to continue processing
            }
        }
    }

    /**
     * Called when a viewer disconnects.
     *
     * @param viewer the disconnected viewer
     */
    public void onViewerDisconnect(@NotNull Viewer viewer) {
        viewerVisibleNPCs.remove(viewer.id());
    }

    /**
     * Called when an NPC is removed.
     *
     * @param npc the removed NPC
     */
    public void onNPCRemoved(@NotNull NPC<?> npc) {
        // Remove from all viewer visible sets
        for (Set<UUID> visibleSet : viewerVisibleNPCs.values()) {
            visibleSet.remove(npc.getId());
        }
    }

    /**
     * Forces an NPC to be visible to a viewer.
     *
     * @param npc    the NPC
     * @param viewer the viewer
     */
    public void forceVisible(@NotNull NPC<?> npc, @NotNull Viewer viewer) {
        viewerVisibleNPCs.computeIfAbsent(viewer.id(), k -> ConcurrentHashMap.newKeySet())
                .add(npc.getId());
    }

    /**
     * Forces an NPC to be hidden from a viewer.
     *
     * @param npc    the NPC
     * @param viewer the viewer
     */
    public void forceHidden(@NotNull NPC<?> npc, @NotNull Viewer viewer) {
        Set<UUID> visible = viewerVisibleNPCs.get(viewer.id());
        if (visible != null) {
            visible.remove(npc.getId());
        }
    }

    /**
     * Checks if an NPC is currently visible to a viewer.
     *
     * @param npc    the NPC
     * @param viewer the viewer
     * @return true if visible
     */
    public boolean isVisible(@NotNull NPC<?> npc, @NotNull Viewer viewer) {
        Set<UUID> visible = viewerVisibleNPCs.get(viewer.id());
        return visible != null && visible.contains(npc.getId());
    }

    /**
     * Updates the view distance and recalculates all visibility.
     * This triggers an immediate visibility recalculation for all NPCs and viewers.
     *
     * @param viewDistance the new view distance in blocks
     */
    public void setViewDistance(double viewDistance) {
        this.viewDistanceSquared = viewDistance * viewDistance;
        // Clear all visibility data to force recalculation
        viewerVisibleNPCs.clear();
    }

    /**
     * Gets the current view distance.
     *
     * @return view distance in blocks
     */
    public double getViewDistance() {
        return Math.sqrt(viewDistanceSquared);
    }

    /**
     * Gets the number of pending results.
     *
     * @return pending result count
     */
    public int pendingResults() {
        return resultQueue.size();
    }

    /**
     * Gets the number of calculations performed.
     *
     * @return calculation count
     */
    public long calculationsPerformed() {
        return calculationsPerformed;
    }

    /**
     * Gets the average calculation time in milliseconds.
     *
     * @return average calculation time
     */
    public double averageCalculationTimeMs() {
        return calculationsPerformed > 0
                ? (double) totalCalculationTimeNs / calculationsPerformed / 1_000_000.0
                : 0.0;
    }

    /**
     * Gets the number of spawns triggered.
     *
     * @return spawn count
     */
    public long spawnsTriggered() {
        return spawnsTriggered;
    }

    /**
     * Gets the number of despawns triggered.
     *
     * @return despawn count
     */
    public long despawnsTriggered() {
        return despawnsTriggered;
    }

    /**
     * Checks if the calculator is running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Resets statistics.
     */
    public void resetStats() {
        calculationsPerformed = 0;
        totalCalculationTimeNs = 0;
        spawnsTriggered = 0;
        despawnsTriggered = 0;
    }

    /**
     * Clears all visibility tracking.
     */
    public void clear() {
        resultQueue.clear();
        viewerVisibleNPCs.clear();
    }
}
