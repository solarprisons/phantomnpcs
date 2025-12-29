package prisons.solar.npclib.api.ai.pathfinding;

import prisons.solar.npclib.api.npc.Position;

import java.util.concurrent.CompletableFuture;

/**
 * Service component for pathfinding operations.
 *
 * <p>The pathfinding component provides methods to find paths between positions
 * in the world. It supports both asynchronous and synchronous pathfinding.
 *
 * <p>Example usage:
 * <pre>{@code
 * PathfindingComponent pathfinder = phantom.services().get(PathfindingComponent.class).orElseThrow();
 *
 * PathRequest request = PathRequest.builder(startPos, goalPos)
 *     .capabilities(MovementCapabilities.DEFAULT)
 *     .allowPartial(true)
 *     .build();
 *
 * // Async pathfinding
 * pathfinder.findPath(request).thenAccept(result -> {
 *     if (result.hasPath()) {
 *         // Use result.path()
 *     }
 * });
 * }</pre>
 *
 * @see PathRequest
 * @see PathResult
 * @see MovementCapabilities
 */
public interface PathfindingComponent {

    /**
     * Asynchronously finds a path based on the request.
     *
     * @param request the path request configuration
     * @return a future that completes with the path result
     */
    CompletableFuture<PathResult> findPath(PathRequest request);

    /**
     * Synchronously finds a path based on the request.
     * Blocks the calling thread until pathfinding completes.
     *
     * <p><strong>Warning:</strong> Should not be called from the main server thread
     * as it may cause lag spikes for complex paths.
     *
     * @param request the path request configuration
     * @return the path result
     */
    PathResult findPathSync(PathRequest request);

    /**
     * Checks if a position is reachable from another position.
     *
     * <p>This is a quick check that does not compute the full path,
     * making it suitable for validation before expensive pathfinding.
     *
     * @param from the starting position
     * @param to   the target position
     * @param caps the movement capabilities to consider
     * @return true if the target is potentially reachable
     */
    boolean isReachable(Position from, Position to, MovementCapabilities caps);
}
