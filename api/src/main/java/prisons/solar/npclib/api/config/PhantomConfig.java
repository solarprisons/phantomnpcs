package prisons.solar.npclib.api.config;

import org.jetbrains.annotations.NotNull;

/**
 * Configuration for the Phantom NPC library.
 */
public interface PhantomConfig {

    /**
     * Gets the default view distance for NPCs in blocks.
     *
     * @return the view distance
     */
    double viewDistance();

    /**
     * Gets the visibility tick rate (how often visibility is updated).
     *
     * @return the tick rate in server ticks
     */
    int visibilityTickRate();

    /**
     * Gets the AI tick rate (how often AI goals are processed).
     *
     * @return the tick rate in server ticks
     */
    int aiTickRate();

    /**
     * Gets the proximity tick rate (how often proximity triggers are checked).
     *
     * @return the tick rate in server ticks
     */
    int proximityTickRate();

    /**
     * Whether to use async visibility calculations.
     *
     * @return true if async visibility is enabled
     */
    boolean asyncVisibility();

    /**
     * Whether to use packet batching for better performance.
     *
     * @return true if packet batching is enabled
     */
    boolean packetBatching();

    /**
     * Gets the packet batch size (max packets per batch).
     *
     * @return the batch size
     */
    int packetBatchSize();

    /**
     * Gets the packet batch flush interval in milliseconds.
     *
     * @return the flush interval
     */
    int packetBatchFlushInterval();

    /**
     * Gets the skin cache duration in minutes.
     *
     * @return the cache duration
     */
    int skinCacheDuration();

    /**
     * Whether debug mode is enabled.
     *
     * @return true if debug mode is enabled
     */
    boolean debug();

    /**
     * Whether update checking is enabled.
     *
     * @return true if update checking is enabled
     */
    boolean updateChecker();

    /**
     * Creates a builder for configuration.
     *
     * @return new builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link PhantomConfig}.
     */
    class Builder {
        private double viewDistance = 48.0;
        private int visibilityTickRate = 10;
        private int aiTickRate = 1;
        private int proximityTickRate = 2;
        private boolean asyncVisibility = true;
        private boolean packetBatching = true;
        private int packetBatchSize = 64;
        private int packetBatchFlushInterval = 50;
        private int skinCacheDuration = 60;
        private boolean debug = false;
        private boolean updateChecker = true;

        public Builder viewDistance(double distance) {
            this.viewDistance = distance;
            return this;
        }

        public Builder visibilityTickRate(int rate) {
            this.visibilityTickRate = rate;
            return this;
        }

        public Builder aiTickRate(int rate) {
            this.aiTickRate = rate;
            return this;
        }

        public Builder proximityTickRate(int rate) {
            this.proximityTickRate = rate;
            return this;
        }

        public Builder asyncVisibility(boolean enabled) {
            this.asyncVisibility = enabled;
            return this;
        }

        public Builder packetBatching(boolean enabled) {
            this.packetBatching = enabled;
            return this;
        }

        public Builder packetBatchSize(int size) {
            this.packetBatchSize = size;
            return this;
        }

        public Builder packetBatchFlushInterval(int interval) {
            this.packetBatchFlushInterval = interval;
            return this;
        }

        public Builder skinCacheDuration(int minutes) {
            this.skinCacheDuration = minutes;
            return this;
        }

        public Builder debug(boolean enabled) {
            this.debug = enabled;
            return this;
        }

        public Builder updateChecker(boolean enabled) {
            this.updateChecker = enabled;
            return this;
        }

        public PhantomConfig build() {
            return new SimplePhantomConfig(
                    viewDistance, visibilityTickRate, aiTickRate, proximityTickRate,
                    asyncVisibility, packetBatching, packetBatchSize, packetBatchFlushInterval,
                    skinCacheDuration, debug, updateChecker
            );
        }
    }

    /**
     * Simple immutable implementation.
     */
    record SimplePhantomConfig(
            double viewDistance,
            int visibilityTickRate,
            int aiTickRate,
            int proximityTickRate,
            boolean asyncVisibility,
            boolean packetBatching,
            int packetBatchSize,
            int packetBatchFlushInterval,
            int skinCacheDuration,
            boolean debug,
            boolean updateChecker
    ) implements PhantomConfig {}
}
