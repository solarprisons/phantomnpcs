package prisons.solar.npclib.core.ai.pathfinding;

import prisons.solar.npclib.api.ai.pathfinding.MovementCapabilities;
import prisons.solar.npclib.api.ai.pathfinding.Path;
import prisons.solar.npclib.api.ai.pathfinding.SimplePath;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.world.BlockPosition;
import prisons.solar.npclib.api.world.WorldProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-processes paths to remove redundant waypoints using line-of-sight raycasting.
 * Reduces waypoint count and creates more direct paths.
 */
public final class PathSmoother {

    private static final double CORNER_PADDING = 0.5;
    private static final int MAX_LOS_ITERATIONS = 100;

    private final WorldProvider worldProvider;

    public PathSmoother(WorldProvider worldProvider) {
        this.worldProvider = worldProvider;
    }

    /**
     * Smooths a path by removing redundant waypoints, then adds corner padding.
     *
     * @param path original path
     * @param caps movement capabilities
     * @return smoothed path with corner padding
     */
    public Path smoothPath(Path path, MovementCapabilities caps) {
        List<Position> waypoints = path.waypoints();
        if (waypoints.size() <= 2) {
            return path;
        }

        List<Position> smoothed = new ArrayList<>();
        smoothed.add(waypoints.get(0));

        int currentIndex = 0;

        while (currentIndex < waypoints.size() - 1) {
            int farthestReachable = currentIndex + 1;

            for (int i = currentIndex + 2; i < waypoints.size(); i++) {
                // Check if ANY intermediate waypoint has a different Y level
                // This prevents smoothing over jump/fall waypoints even if start/end are same Y
                if (hasVerticalTransitionBetween(waypoints, currentIndex, i)) {
                    break;  // Can't smooth past a vertical transition
                }

                if (hasLineOfSight(waypoints.get(currentIndex), waypoints.get(i), caps)) {
                    farthestReachable = i;
                } else {
                    break;
                }
            }

            currentIndex = farthestReachable;
            smoothed.add(waypoints.get(currentIndex));
        }

        // Add corner padding to prevent clipping walls at turns
        return addCornerPadding(new SimplePath(smoothed));
    }

    /**
     * Checks if there's a vertical transition (jump/fall waypoint) between two indices.
     * Prevents smoothing from skipping over waypoints that require vertical movement.
     */
    private boolean hasVerticalTransitionBetween(List<Position> waypoints, int fromIndex, int toIndex) {
        int baseBlockY = (int) Math.floor(waypoints.get(fromIndex).y());

        for (int i = fromIndex + 1; i < toIndex; i++) {
            int waypointBlockY = (int) Math.floor(waypoints.get(i).y());
            if (waypointBlockY != baseBlockY) {
                return true;  // Found a waypoint at different Y level
            }
        }
        return false;
    }

    /**
     * Adds padding to corner waypoints to prevent NPCs from clipping walls.
     * When a path makes a 90-degree turn around a solid block, the corner waypoint
     * is offset diagonally away from the wall to give clearance.
     *
     * @param path the path to add padding to
     * @return path with corner padding applied
     */
    private Path addCornerPadding(Path path) {
        List<Position> waypoints = path.waypoints();
        if (waypoints.size() < 3) {
            return path;
        }

        List<Position> result = new ArrayList<>();
        result.add(waypoints.get(0));

        for (int i = 1; i < waypoints.size() - 1; i++) {
            Position prev = waypoints.get(i - 1);
            Position curr = waypoints.get(i);
            Position next = waypoints.get(i + 1);

            // Skip if not on same Y level (vertical transitions handled separately)
            if (Math.floor(prev.y()) != Math.floor(curr.y()) ||
                Math.floor(curr.y()) != Math.floor(next.y())) {
                result.add(curr);
                continue;
            }

            // Calculate direction vectors
            int prevDx = (int) Math.signum(curr.x() - prev.x());
            int prevDz = (int) Math.signum(curr.z() - prev.z());
            int nextDx = (int) Math.signum(next.x() - curr.x());
            int nextDz = (int) Math.signum(next.z() - curr.z());

            // Check for 90-degree turn (direction changes on one axis)
            boolean isTurn = (prevDx != nextDx || prevDz != nextDz) &&
                             (prevDx != 0 || prevDz != 0) &&
                             (nextDx != 0 || nextDz != 0);

            if (!isTurn) {
                result.add(curr);
                continue;
            }

            // Find the inside corner block that would be clipped
            int cornerX = (int) Math.floor(curr.x());
            int cornerZ = (int) Math.floor(curr.z());

            // The inside corner is in the direction we came from + direction we're going
            if (prevDx != 0 && nextDz != 0) {
                cornerX += prevDx;
                cornerZ += nextDz;
            } else if (prevDz != 0 && nextDx != 0) {
                cornerX += nextDx;
                cornerZ += prevDz;
            }

            BlockPosition insideCorner = BlockPosition.of(
                curr.worldId(), cornerX, (int) Math.floor(curr.y()), cornerZ);

            if (worldProvider.isBlockSolid(insideCorner)) {
                // Insert TWO waypoints for corner turns to prevent clipping:
                // 1. Approach waypoint - offset perpendicular to approach direction
                // 2. Exit waypoint - full diagonal offset for clean exit
                double padding = CORNER_PADDING;

                // Calculate offset direction - move AWAY from inside corner
                double offsetX = (cornerX > Math.floor(curr.x())) ? -padding : padding;
                double offsetZ = (cornerZ > Math.floor(curr.z())) ? -padding : padding;

                // Waypoint 1: Approach - offset only on axis perpendicular to approach direction
                double approachX = curr.x();
                double approachZ = curr.z();
                if (prevDx != 0) {
                    // Approaching along X axis, offset on Z
                    approachZ += offsetZ;
                } else {
                    // Approaching along Z axis, offset on X
                    approachX += offsetX;
                }
                result.add(new Position(curr.worldId(), approachX, curr.y(), approachZ,
                    curr.yaw(), curr.pitch()));

                // Waypoint 2: Exit - full diagonal offset for clean exit
                result.add(new Position(curr.worldId(), curr.x() + offsetX, curr.y(),
                    curr.z() + offsetZ, curr.yaw(), curr.pitch()));
            } else {
                result.add(curr);
            }
        }

        result.add(waypoints.get(waypoints.size() - 1));
        return new SimplePath(result);
    }

    /**
     * Checks if there is a direct line-of-sight between two positions using 3D DDA.
     * Uses proper line rasterization to check ALL blocks the line passes through,
     * preventing diagonal paths from skipping over obstacles.
     *
     * @param from starting position
     * @param to ending position
     * @param caps movement capabilities
     * @return true if direct path exists, false otherwise
     */
    private boolean hasLineOfSight(Position from, Position to, MovementCapabilities caps) {
        // Block Y coordinates for vertical validation - fractional Y doesn't affect block traversal
        int fromBlockY = (int) Math.floor(from.y());
        int toBlockY = (int) Math.floor(to.y());
        int blockDy = toBlockY - fromBlockY;

        // NEVER smooth across vertical transitions - preserve jump/fall waypoints
        // The NPC needs to be at the exact position before a jump to initiate it correctly
        if (blockDy != 0) {
            return false;
        }

        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double dz = to.z() - from.z();

        int x = (int) Math.floor(from.x());
        int y = (int) Math.floor(from.y());
        int z = (int) Math.floor(from.z());

        int endX = (int) Math.floor(to.x());
        int endY = (int) Math.floor(to.y());
        int endZ = (int) Math.floor(to.z());

        int stepX = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int stepY = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        int stepZ = dz > 0 ? 1 : (dz < 0 ? -1 : 0);

        double tDeltaX = (dx == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dx);
        double tDeltaY = (dy == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dy);
        double tDeltaZ = (dz == 0) ? Double.MAX_VALUE : Math.abs(1.0 / dz);

        double tMaxX = (dx == 0) ? Double.MAX_VALUE :
            (stepX > 0 ? (x + 1 - from.x()) : (from.x() - x)) * tDeltaX;
        double tMaxY = (dy == 0) ? Double.MAX_VALUE :
            (stepY > 0 ? (y + 1 - from.y()) : (from.y() - y)) * tDeltaY;
        double tMaxZ = (dz == 0) ? Double.MAX_VALUE :
            (stepZ > 0 ? (z + 1 - from.z()) : (from.z() - z)) * tDeltaZ;

        int maxIterations = MAX_LOS_ITERATIONS;
        int headClearanceBlocks = (int) Math.ceil(caps.entityHeight());
        int prevX = x, prevZ = z;
        String worldId = from.worldId();

        for (int i = 0; i < maxIterations; i++) {
            BlockPosition block = BlockPosition.of(worldId, x, y, z);
            if (worldProvider.isBlockSolid(block)) {
                return false;
            }

            // Check ground exists - can't smooth over pits/gaps
            // NPC would fall into the pit, so we need the original waypoints
            BlockPosition groundBlock = BlockPosition.of(worldId, x, y - 1, z);
            if (!worldProvider.isBlockSolid(groundBlock)) {
                return false;  // No ground = can't walk here
            }

            for (int headY = 1; headY <= headClearanceBlocks; headY++) {
                if (worldProvider.isBlockSolid(block.add(0, headY, 0))) {
                    return false;
                }
            }

            // Check diagonal corner blocking - can't squeeze through two adjacent solid blocks
            // This matches MovementValidator's diagonal check to prevent smoothed paths
            // from creating moves that collision would block
            if (x != prevX && z != prevZ) {
                BlockPosition sideX = BlockPosition.of(worldId, x, y, prevZ);
                BlockPosition sideZ = BlockPosition.of(worldId, prevX, y, z);
                if (worldProvider.isBlockSolid(sideX) && worldProvider.isBlockSolid(sideZ)) {
                    return false;
                }
                // Also check head level
                if (worldProvider.isBlockSolid(sideX.add(0, 1, 0)) &&
                    worldProvider.isBlockSolid(sideZ.add(0, 1, 0))) {
                    return false;
                }
            }

            if (x == endX && y == endY && z == endZ) {
                return true;
            }

            prevX = x;
            prevZ = z;

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                tMaxX += tDeltaX;
                x += stepX;
            } else if (tMaxY < tMaxZ) {
                tMaxY += tDeltaY;
                y += stepY;
            } else {
                tMaxZ += tDeltaZ;
                z += stepZ;
            }
        }

        return false;
    }
}
