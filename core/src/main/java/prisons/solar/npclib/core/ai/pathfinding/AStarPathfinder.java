package prisons.solar.npclib.core.ai.pathfinding;

import prisons.solar.npclib.api.ai.pathfinding.*;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.world.BlockPosition;
import prisons.solar.npclib.api.world.WorldProvider;
import prisons.solar.npclib.core.physics.CollisionEntity;
import prisons.solar.npclib.core.physics.CollisionManager;
import prisons.solar.npclib.core.spatial.SpatialGrid;

import java.util.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A* pathfinder with 8-directional + vertical neighbor exploration.
 * Supports NPC-to-NPC avoidance via optional CollisionManager integration.
 */
public final class AStarPathfinder implements PathfindingComponent {

    private static final int TIMEOUT_CHECK_INTERVAL = 50;
    private static final long REACHABLE_CHECK_TIMEOUT_MS = 50;
    private static final int REACHABLE_CHECK_MAX_PATH_LENGTH = 128;
    private static final double MIN_JUMP_HEIGHT = 1.0;

    private final WorldProvider worldProvider;
    private final MovementValidator moveValidator;
    private final ThreadLocal<NodePool> nodePoolThreadLocal;
    private final ClosestReachableFinder closestReachableFinder;
    private final PathSmoother pathSmoother;
    private final Executor executor;
    private final CollisionManager collisionManager;

    public AStarPathfinder(WorldProvider worldProvider, Executor executor) {
        this(worldProvider, executor, null);
    }

    public AStarPathfinder(WorldProvider worldProvider, Executor executor, CollisionManager collisionManager) {
        this.worldProvider = worldProvider;
        this.moveValidator = new MovementValidator(worldProvider);
        this.nodePoolThreadLocal = ThreadLocal.withInitial(NodePool::new);
        this.closestReachableFinder = new ClosestReachableFinder(worldProvider);
        this.pathSmoother = new PathSmoother(worldProvider);
        this.executor = executor;
        this.collisionManager = collisionManager;
    }

    @Override
    public CompletableFuture<PathResult> findPath(PathRequest request) {
        return CompletableFuture.supplyAsync(() -> findPathSync(request), executor);
    }

    @Override
    public PathResult findPathSync(PathRequest request) {
        if (!request.start().worldId().equals(request.goal().worldId())) {
            return PathResult.differentWorld();
        }

        BlockPosition start = BlockPosition.from(request.start());
        BlockPosition goal = BlockPosition.from(request.goal());

        if (start.equals(goal)) {
            return PathResult.samePosition();
        }

        long startTime = System.currentTimeMillis();
        long timeout = request.maxComputeTime();

        NodePool pool = nodePoolThreadLocal.get();
        pool.reset();

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<BlockPosition, Node> openMap = new HashMap<>();
        Set<BlockPosition> closedSet = new HashSet<>();

        double h = heuristic(start, goal);
        Node startNode = pool.acquire(start, null, 0, h);
        openSet.add(startNode);
        openMap.put(start, startNode);

        Node bestNode = startNode;
        double bestDistance = h;

        int nodesExplored = 0;
        int maxNodes = Math.min(request.maxPathLength() * 2, pool.capacity());

        while (!openSet.isEmpty() && nodesExplored < maxNodes) {
            if (nodesExplored % TIMEOUT_CHECK_INTERVAL == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > timeout) {
                    return handleTimeout(bestNode, startNode, request, nodesExplored, elapsed);
                }
            }

            Node current = openSet.poll();
            if (current == null) break;

            openMap.remove(current.position);

            if (current.position.equals(goal)) {
                Path path = reconstructPath(current, request.start());
                path = pathSmoother.smoothPath(path, request.capabilities());
                long elapsed = System.currentTimeMillis() - startTime;
                return PathResult.success(path, nodesExplored, elapsed);
            }

            closedSet.add(current.position);
            nodesExplored++;

            double distToGoal = heuristic(current.position, goal);
            if (distToGoal < bestDistance) {
                bestDistance = distToGoal;
                bestNode = current;
            }

            exploreNeighbors(current, goal, request.capabilities(), openSet, openMap, closedSet, pool, request.excludeNpcId());
        }

        long elapsed = System.currentTimeMillis() - startTime;

        if (bestNode != startNode && request.allowPartial()) {
            Path path = reconstructPath(bestNode, request.start());
            path = pathSmoother.smoothPath(path, request.capabilities());
            return PathResult.partial(path, nodesExplored, elapsed);
        }

        Position closestReachable = closestReachableFinder.findClosestReachable(
            request.start(),
            request.goal(),
            request.capabilities()
        );

        if (!closestReachable.equals(request.start())) {
            PathRequest newRequest = PathRequest.builder(request.start(), closestReachable)
                .capabilities(request.capabilities())
                .maxComputeTime(timeout - elapsed)
                .maxPathLength(request.maxPathLength())
                .allowPartial(false)
                .priority(request.priority())
                .build();

            return findPathSync(newRequest);
        }
        return PathResult.unreachable(nodesExplored, elapsed);
    }

    @Override
    public boolean isReachable(Position from, Position to, MovementCapabilities caps) {
        PathRequest request = PathRequest.builder(from, to)
            .capabilities(caps)
            .maxComputeTime(REACHABLE_CHECK_TIMEOUT_MS)
            .maxPathLength(REACHABLE_CHECK_MAX_PATH_LENGTH)
            .allowPartial(false)
            .build();

        PathResult result = findPathSync(request);
        return result.status() == PathResult.Status.SUCCESS;
    }

    private void exploreNeighbors(Node current, BlockPosition goal, MovementCapabilities caps,
                                  PriorityQueue<Node> openSet, Map<BlockPosition, Node> openMap,
                                  Set<BlockPosition> closedSet, NodePool pool, UUID excludeNpcId) {

        // Check if we're in water for swimming exploration
        boolean inWater = worldProvider.isBlockWater(current.position);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;

                BlockPosition walkPos = current.position.add(dx, 0, dz);
                boolean walkSucceeded = tryAddNeighbor(walkPos, current, goal, caps, openSet, openMap, closedSet, pool, excludeNpcId);

                if (!walkSucceeded) {
                    if (caps.jumpHeight() >= MIN_JUMP_HEIGHT) {
                        BlockPosition jumpPos = current.position.add(dx, 1, dz);
                        tryAddNeighbor(jumpPos, current, goal, caps, openSet, openMap, closedSet, pool, excludeNpcId);
                    }

                    BlockPosition fallPos = current.position.add(dx, -1, dz);
                    tryAddNeighbor(fallPos, current, goal, caps, openSet, openMap, closedSet, pool, excludeNpcId);
                }
            }
        }

        // In water, also explore straight up/down for swimming
        if (inWater && caps.canSwim()) {
            BlockPosition upPos = current.position.add(0, 1, 0);
            BlockPosition downPos = current.position.add(0, -1, 0);
            tryAddNeighbor(upPos, current, goal, caps, openSet, openMap, closedSet, pool, excludeNpcId);
            tryAddNeighbor(downPos, current, goal, caps, openSet, openMap, closedSet, pool, excludeNpcId);
        }
    }

    private boolean tryAddNeighbor(BlockPosition next, Node current, BlockPosition goal,
                                   MovementCapabilities caps, PriorityQueue<Node> openSet,
                                   Map<BlockPosition, Node> openMap, Set<BlockPosition> closedSet,
                                   NodePool pool, UUID excludeNpcId) {

        if (closedSet.contains(next)) return false;
        if (!pool.hasCapacity()) return false;

        MovementValidator.MoveResult result = moveValidator.validateMove(current.position, next, caps);
        if (result.isBlocked()) return false;

        // Check for NPC obstacles (skip if no collision manager or no exclude ID)
        if (isBlockedByNPC(next, excludeNpcId)) return false;

        double tentativeG = current.g + result.cost();

        Node existing = openMap.get(next);
        if (existing != null && tentativeG >= existing.g) {
            return false;
        }

        double h = heuristic(next, goal);
        Node newNode = pool.acquire(next, current, tentativeG, h);

        if (existing != null) {
            openSet.remove(existing);
        }

        openSet.add(newNode);
        openMap.put(next, newNode);
        return true;
    }

    /**
     * Checks if a block position is occupied by another NPC.
     *
     * @param pos block position to check
     * @param excludeId ID of the requesting NPC to exclude from check
     * @return true if blocked by another NPC, false otherwise
     */
    private boolean isBlockedByNPC(BlockPosition pos, UUID excludeId) {
        if (collisionManager == null || excludeId == null) return false;

        SpatialGrid grid = collisionManager.getGrid(pos.worldId());
        if (grid == null) return false;

        Collection<CollisionEntity> nearby = grid.findInRange(
            pos.worldId(), pos.x() + 0.5, pos.y(), pos.z() + 0.5, 1.0);

        for (CollisionEntity entity : nearby) {
            if (entity.getId().equals(excludeId)) continue;

            Position npcPos = entity.getPosition();
            if ((int) Math.floor(npcPos.x()) == pos.x() &&
                (int) Math.floor(npcPos.y()) == pos.y() &&
                (int) Math.floor(npcPos.z()) == pos.z()) {
                return true;
            }
        }
        return false;
    }

    private double heuristic(BlockPosition from, BlockPosition to) {
        int dx = Math.abs(to.x() - from.x());
        int dy = Math.abs(to.y() - from.y());
        int dz = Math.abs(to.z() - from.z());

        // Octile distance - accurate for 8-directional movement
        int diagonal = Math.min(dx, dz);
        int straight = (dx + dz) - 2 * diagonal;

        return 1.414 * diagonal + straight + dy;
    }

    private Path reconstructPath(Node goalNode, Position startPosition) {
        List<Position> waypoints = new ArrayList<>();

        Node current = goalNode;
        while (current != null) {
            Position pos;
            if (current.parent == null) {
                // Start node: use actual NPC position, not block center
                // This prevents edge-position NPCs from having a "backwards" first waypoint
                pos = new Position(
                    startPosition.worldId(),
                    startPosition.x(),
                    current.position.y(),  // Y from pathfinder (will be corrected later)
                    startPosition.z(),
                    0, 0
                );
            } else {
                pos = new Position(
                    startPosition.worldId(),
                    current.position.x() + 0.5,
                    current.position.y(),
                    current.position.z() + 0.5,
                    0, 0
                );
            }
            waypoints.add(pos);
            current = current.parent;
        }

        Collections.reverse(waypoints);
        return new SimplePath(waypoints);
    }

    private PathResult handleTimeout(Node bestNode, Node startNode, PathRequest request,
                                     int nodesExplored, long elapsed) {
        if (bestNode != startNode && request.allowPartial()) {
            Path path = reconstructPath(bestNode, request.start());
            path = pathSmoother.smoothPath(path, request.capabilities());
            return PathResult.partial(path, nodesExplored, elapsed);
        }
        return PathResult.timeout(nodesExplored, elapsed);
    }
}
