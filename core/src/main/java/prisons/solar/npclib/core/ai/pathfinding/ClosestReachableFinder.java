package prisons.solar.npclib.core.ai.pathfinding;

import prisons.solar.npclib.api.ai.pathfinding.MovementCapabilities;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.world.BlockPosition;
import prisons.solar.npclib.api.world.WorldProvider;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Finds the closest walkable position to an unreachable goal using spiral BFS search.
 * Used when pathfinding fails to find a complete path to the goal.
 */
public final class ClosestReachableFinder {
    private final WorldProvider worldProvider;

    private static final int MAX_SEARCH_RADIUS = 8;
    private static final int MAX_ITERATIONS = 500;

    public ClosestReachableFinder(WorldProvider worldProvider) {
        this.worldProvider = worldProvider;
    }

    /**
     * Finds the closest reachable position to an unreachable goal.
     *
     * @param start starting position
     * @param unreachableGoal goal position that is unreachable
     * @param caps movement capabilities
     * @return closest walkable position within 8 blocks, or start if none found
     */
    public Position findClosestReachable(Position start, Position unreachableGoal, MovementCapabilities caps) {
        BlockPosition goalBlock = BlockPosition.from(unreachableGoal);

        Queue<BlockPosition> queue = new ArrayDeque<>();
        Set<BlockPosition> visited = new HashSet<>();

        queue.add(goalBlock);
        visited.add(goalBlock);

        int iterations = 0;

        while (!queue.isEmpty() && iterations < MAX_ITERATIONS) {
            BlockPosition current = queue.poll();
            if (current == null) continue;

            iterations++;

            if (isWalkable(current, caps)) {
                return new Position(
                    unreachableGoal.worldId(),
                    current.x() + 0.5,
                    current.y(),
                    current.z() + 0.5,
                    0, 0
                );
            }

            double distance = Math.sqrt(
                Math.pow(current.x() - goalBlock.x(), 2) +
                Math.pow(current.z() - goalBlock.z(), 2)
            );
            if (distance > MAX_SEARCH_RADIUS) continue;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;

                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPosition neighbor = current.add(dx, dy, dz);

                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return start;
    }

    /**
     * Checks if a position is walkable for an NPC.
     *
     * @param pos position to check
     * @param caps movement capabilities
     * @return true if walkable, false otherwise
     */
    private boolean isWalkable(BlockPosition pos, MovementCapabilities caps) {
        if (worldProvider.isBlockSolid(pos)) {
            return false;
        }

        int headClearanceBlocks = (int) Math.ceil(caps.entityHeight());
        for (int y = 1; y <= headClearanceBlocks; y++) {
            BlockPosition headBlock = pos.add(0, y, 0);
            if (worldProvider.isBlockSolid(headBlock)) {
                return false;
            }
        }

        BlockPosition below = pos.add(0, -1, 0);
        if (worldProvider.isBlockSolid(below)) {
            return true;
        }

        int fallDistance = calculateFallDistance(pos, (int) caps.fallTolerance() + 1);
        return fallDistance <= caps.fallTolerance();
    }

    private int calculateFallDistance(BlockPosition from, int maxDistance) {
        for (int i = 1; i <= maxDistance; i++) {
            BlockPosition check = from.add(0, -i, 0);
            if (worldProvider.isBlockSolid(check)) {
                return i - 1;
            }
        }
        return maxDistance;
    }
}
