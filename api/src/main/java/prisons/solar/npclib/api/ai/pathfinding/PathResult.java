package prisons.solar.npclib.api.ai.pathfinding;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Result of a pathfinding operation.
 *
 * <p>Contains the computed path (if any), the status of the operation,
 * and metrics about the computation.
 *
 * <p>Example usage:
 * <pre>{@code
 * PathResult result = pathfinder.findPathSync(request);
 *
 * if (result.isSuccess()) {
 *     Path path = result.path();
 *     // Navigate along path waypoints
 * } else if (result.status() == Status.PARTIAL && result.hasPath()) {
 *     // Partial path - can navigate to closest reachable point
 * } else {
 *     // Handle failure (UNREACHABLE, TIMEOUT, etc.)
 * }
 * }</pre>
 *
 * @param status        the result status
 * @param path          the computed path, or null if pathfinding failed
 * @param nodesExplored number of nodes explored during pathfinding
 * @param computeTimeMs time spent computing the path in milliseconds
 */
public record PathResult(
        @NotNull Status status,
        @Nullable Path path,
        int nodesExplored,
        long computeTimeMs
) {

    /**
     * Status codes for pathfinding results.
     */
    public enum Status {
        /** Path successfully found to exact goal */
        SUCCESS,
        /** Partial path found - goal unreachable but closest point returned */
        PARTIAL,
        /** Goal is completely unreachable */
        UNREACHABLE,
        /** Computation timed out before finding path */
        TIMEOUT,
        /** Start and goal positions are the same */
        SAME_POSITION,
        /** Start and goal are in different worlds */
        DIFFERENT_WORLD
    }

    /**
     * Validates the path result parameters.
     */
    public PathResult {
        if (nodesExplored < 0) {
            throw new IllegalArgumentException("PathResult.nodesExplored cannot be negative");
        }
        if (computeTimeMs < 0) {
            throw new IllegalArgumentException("PathResult.computeTimeMs cannot be negative");
        }
    }

    /**
     * Creates a successful path result.
     *
     * @param path          the computed path
     * @param nodesExplored nodes explored during computation
     * @param computeTimeMs computation time in milliseconds
     * @return a success result
     */
    public static PathResult success(Path path, int nodesExplored, long computeTimeMs) {
        return new PathResult(Status.SUCCESS, path, nodesExplored, computeTimeMs);
    }

    /**
     * Creates a partial path result (goal unreachable, but partial path available).
     *
     * @param path          the partial path to closest reachable point
     * @param nodesExplored nodes explored during computation
     * @param computeTimeMs computation time in milliseconds
     * @return a partial result
     */
    public static PathResult partial(Path path, int nodesExplored, long computeTimeMs) {
        return new PathResult(Status.PARTIAL, path, nodesExplored, computeTimeMs);
    }

    /**
     * Creates an unreachable result (no path exists).
     *
     * @param nodesExplored nodes explored during computation
     * @param computeTimeMs computation time in milliseconds
     * @return an unreachable result
     */
    public static PathResult unreachable(int nodesExplored, long computeTimeMs) {
        return new PathResult(Status.UNREACHABLE, null, nodesExplored, computeTimeMs);
    }

    /**
     * Creates a timeout result (computation exceeded time limit).
     *
     * @param nodesExplored nodes explored before timeout
     * @param computeTimeMs computation time in milliseconds
     * @return a timeout result
     */
    public static PathResult timeout(int nodesExplored, long computeTimeMs) {
        return new PathResult(Status.TIMEOUT, null, nodesExplored, computeTimeMs);
    }

    /**
     * Creates a same-position result (start equals goal).
     *
     * @return a same-position result
     */
    public static PathResult samePosition() {
        return new PathResult(Status.SAME_POSITION, null, 0, 0);
    }

    /**
     * Creates a different-world result (start and goal in different worlds).
     *
     * @return a different-world result
     */
    public static PathResult differentWorld() {
        return new PathResult(Status.DIFFERENT_WORLD, null, 0, 0);
    }

    /**
     * Checks if the pathfinding was fully successful.
     *
     * @return true if status is SUCCESS
     */
    public boolean isSuccess() {
        return status.equals(Status.SUCCESS);
    }

    /**
     * Checks if a path was computed (may be partial).
     *
     * @return true if path is not null
     */
    public boolean hasPath() {
        return path != null;
    }
}
