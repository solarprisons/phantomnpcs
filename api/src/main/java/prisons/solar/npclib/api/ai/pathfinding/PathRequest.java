package prisons.solar.npclib.api.ai.pathfinding;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.npc.Position;

import java.util.UUID;

/**
 * Configuration for a pathfinding request.
 *
 * <p>Use the {@link Builder} to construct path requests with custom settings:
 * <pre>{@code
 * PathRequest request = PathRequest.builder(startPos, goalPos)
 *     .capabilities(MovementCapabilities.DEFAULT)
 *     .maxComputeTime(100)
 *     .maxPathLength(256)
 *     .allowPartial(true)
 *     .priority(PathPriority.HIGH)
 *     .build();
 * }</pre>
 *
 * @param start          the starting position
 * @param goal           the target position (must be in same world as start)
 * @param capabilities   movement capabilities of the entity
 * @param maxComputeTime maximum computation time in milliseconds
 * @param maxPathLength  maximum path length in blocks
 * @param allowPartial   if true, return partial path when goal unreachable
 * @param priority       pathfinding priority for scheduling
 * @param excludeNpcId   NPC to exclude from collision checks (typically self)
 */
public record PathRequest(
        @NotNull Position start,
        @NotNull Position goal,
        @NotNull MovementCapabilities capabilities,
        long maxComputeTime,
        int maxPathLength,
        boolean allowPartial,
        @NotNull PathPriority priority,
        @Nullable UUID excludeNpcId
) {

    /**
     * Creates a new builder for path requests.
     *
     * @param start the starting position
     * @param goal  the goal position
     * @return a new builder
     */
    public static Builder builder(Position start, Position goal) {
        return new Builder(start, goal);
    }

    /**
     * Builder for constructing {@link PathRequest} instances.
     */
    public static class Builder {
        private final Position start;
        private final Position goal;

        private MovementCapabilities capabilities = MovementCapabilities.DEFAULT;
        private long maxComputeTime = 50;
        private int maxPathLength = 512;
        private boolean allowPartial = false;
        private PathPriority priority = PathPriority.NORMAL;
        private UUID excludeNpcId = null;

        /**
         * Creates a builder with required start and goal positions.
         *
         * @param start the starting position
         * @param goal  the goal position
         */
        public Builder(Position start, Position goal) {
            this.start = start;
            this.goal = goal;
        }

        /**
         * Sets the movement capabilities.
         *
         * @param capabilities the movement capabilities
         * @return this builder
         */
        public Builder capabilities(@NotNull MovementCapabilities capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        /**
         * Sets the maximum computation time in milliseconds.
         *
         * @param maxComputeTime max time in ms
         * @return this builder
         */
        public Builder maxComputeTime(long maxComputeTime) {
            this.maxComputeTime = maxComputeTime;
            return this;
        }

        /**
         * Sets the maximum path length in blocks.
         *
         * @param maxPathLength max path length
         * @return this builder
         */
        public Builder maxPathLength(int maxPathLength) {
            this.maxPathLength = maxPathLength;
            return this;
        }

        /**
         * Sets whether to allow partial paths when goal is unreachable.
         *
         * @param allowPartial true to allow partial paths
         * @return this builder
         */
        public Builder allowPartial(boolean allowPartial) {
            this.allowPartial = allowPartial;
            return this;
        }

        /**
         * Sets the pathfinding priority.
         *
         * @param priority the priority level
         * @return this builder
         */
        public Builder priority(PathPriority priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets an NPC ID to exclude from collision checks.
         *
         * @param excludeNpcId the NPC UUID to exclude
         * @return this builder
         */
        public Builder excludeNpcId(@Nullable UUID excludeNpcId) {
            this.excludeNpcId = excludeNpcId;
            return this;
        }

        /**
         * Builds the path request.
         *
         * @return the constructed path request
         */
        public PathRequest build() {
            return new PathRequest(
                    start,
                    goal,
                    capabilities,
                    maxComputeTime,
                    maxPathLength,
                    allowPartial,
                    priority,
                    excludeNpcId
            );
        }
    }

    /**
     * Priority levels for pathfinding requests.
     * Higher priority requests get more computation budget.
     */
    public enum PathPriority {
        /** Critical priority - 100ms budget */
        CRITICAL(100),
        /** Highest priority - 80ms budget */
        HIGHEST(80),
        /** High priority - 60ms budget */
        HIGH(60),
        /** Normal priority - 40ms budget */
        NORMAL(40),
        /** Low priority - 20ms budget */
        LOW(20);

        /** The computation budget in milliseconds */
        public final int budgetMs;

        PathPriority(int budgetMs) {
            this.budgetMs = budgetMs;
        }
    }

    /**
     * Validates the path request parameters.
     */
    public PathRequest {
        if (maxComputeTime < 0) {
            throw new IllegalArgumentException("maxComputeTime cannot be negative: " + maxComputeTime);
        }
        if (maxPathLength < 0) {
            throw new IllegalArgumentException("maxPathLength cannot be negative: " + maxPathLength);
        }
        if (!start.worldId().equals(goal.worldId())) {
            throw new IllegalArgumentException(
                    "Path goal must remain in the same world it started! Start: " +
                            start.worldId() + ", Goal: " + goal.worldId()
            );
        }
    }
}
