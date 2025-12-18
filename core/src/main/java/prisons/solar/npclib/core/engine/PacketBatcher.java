package prisons.solar.npclib.core.engine;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * Batches packets for efficient sending to multiple viewers.
 * Reduces network overhead by combining multiple packets into batched sends.
 *
 * @param <P> the packet type
 * @param <V> the viewer type
 */
public class PacketBatcher<P, V> {

    private final Map<V, Queue<P>> viewerQueues = new ConcurrentHashMap<>();
    private final BiConsumer<V, List<P>> batchSender;
    private final int batchSize;
    private final int flushIntervalMs;

    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    // Statistics
    private long packetsSent = 0;
    private long batchesSent = 0;

    /**
     * Creates a new packet batcher.
     *
     * @param batchSender     function to send a batch of packets to a viewer
     * @param batchSize       max packets per batch
     * @param flushIntervalMs how often to flush in milliseconds
     */
    public PacketBatcher(@NotNull BiConsumer<V, List<P>> batchSender, int batchSize, int flushIntervalMs) {
        this.batchSender = batchSender;
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
    }

    /**
     * Creates a packet batcher with default settings.
     *
     * @param batchSender function to send a batch of packets to a viewer
     */
    public PacketBatcher(@NotNull BiConsumer<V, List<P>> batchSender) {
        this(batchSender, 64, 50);
    }

    /**
     * Starts the packet batcher.
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Phantom-PacketBatcher");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(this::flush, flushIntervalMs, flushIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the packet batcher and flushes remaining packets.
     */
    public void stop() {
        if (!running) {
            return;
        }
        running = false;

        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Flush remaining packets
        flush();
        viewerQueues.clear();
    }

    /**
     * Queues a packet to be sent to a viewer.
     *
     * @param viewer the viewer
     * @param packet the packet
     */
    public void queue(@NotNull V viewer, @NotNull P packet) {
        if (!running) {
            // If not running, send immediately as single-item batch
            batchSender.accept(viewer, Collections.singletonList(packet));
            packetsSent++;
            batchesSent++;
            return;
        }

        Queue<P> queue = viewerQueues.computeIfAbsent(viewer, k -> new ConcurrentLinkedQueue<>());
        queue.offer(packet);

        // If queue exceeds batch size, flush immediately
        if (queue.size() >= batchSize) {
            flushViewer(viewer);
        }
    }

    /**
     * Queues a packet to be sent to multiple viewers.
     *
     * @param viewers the viewers
     * @param packet  the packet
     */
    public void queueAll(@NotNull Collection<V> viewers, @NotNull P packet) {
        for (V viewer : viewers) {
            queue(viewer, packet);
        }
    }

    /**
     * Queues multiple packets to be sent to a viewer.
     *
     * @param viewer  the viewer
     * @param packets the packets
     */
    public void queueAll(@NotNull V viewer, @NotNull Collection<P> packets) {
        for (P packet : packets) {
            queue(viewer, packet);
        }
    }

    /**
     * Immediately sends a packet without batching.
     *
     * @param viewer the viewer
     * @param packet the packet
     */
    public void sendImmediate(@NotNull V viewer, @NotNull P packet) {
        batchSender.accept(viewer, Collections.singletonList(packet));
        packetsSent++;
        batchesSent++;
    }

    /**
     * Flushes all queued packets.
     */
    public void flush() {
        for (V viewer : viewerQueues.keySet()) {
            flushViewer(viewer);
        }
    }

    /**
     * Flushes packets for a specific viewer.
     *
     * @param viewer the viewer
     */
    public void flushViewer(@NotNull V viewer) {
        Queue<P> queue = viewerQueues.get(viewer);
        if (queue == null || queue.isEmpty()) {
            return;
        }

        List<P> batch = new ArrayList<>(Math.min(queue.size(), batchSize));

        P packet;
        while (batch.size() < batchSize && (packet = queue.poll()) != null) {
            batch.add(packet);
        }

        if (!batch.isEmpty()) {
            try {
                batchSender.accept(viewer, batch);
                packetsSent += batch.size();
                batchesSent++;
            } catch (Exception e) {
                System.err.println("[Phantom] Error sending packet batch: " + e.getMessage());
            }
        }

        // If there are still packets, flush again
        if (!queue.isEmpty()) {
            flushViewer(viewer);
        }
    }

    /**
     * Removes a viewer from the batcher (e.g., on disconnect).
     *
     * @param viewer the viewer to remove
     */
    public void removeViewer(@NotNull V viewer) {
        Queue<P> queue = viewerQueues.remove(viewer);
        if (queue != null) {
            queue.clear();
        }
    }

    /**
     * Gets the number of packets sent.
     *
     * @return packets sent
     */
    public long packetsSent() {
        return packetsSent;
    }

    /**
     * Gets the number of batches sent.
     *
     * @return batches sent
     */
    public long batchesSent() {
        return batchesSent;
    }

    /**
     * Gets the average batch size.
     *
     * @return average packets per batch
     */
    public double averageBatchSize() {
        return batchesSent > 0 ? (double) packetsSent / batchesSent : 0.0;
    }

    /**
     * Gets the current queue size for a viewer.
     *
     * @param viewer the viewer
     * @return queue size
     */
    public int queueSize(@NotNull V viewer) {
        Queue<P> queue = viewerQueues.get(viewer);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Gets the total queued packet count.
     *
     * @return total queued packets
     */
    public int totalQueued() {
        return viewerQueues.values().stream()
                .mapToInt(Queue::size)
                .sum();
    }

    /**
     * Checks if the batcher is running.
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
        packetsSent = 0;
        batchesSent = 0;
    }
}
