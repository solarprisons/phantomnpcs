package prisons.solar.npclib.core.ai.goals;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.ai.Goal;
import prisons.solar.npclib.api.ai.pathfinding.MovementCapabilities;
import prisons.solar.npclib.api.ai.pathfinding.Path;
import prisons.solar.npclib.api.ai.pathfinding.PathRequest;
import prisons.solar.npclib.api.ai.pathfinding.PathResult;
import prisons.solar.npclib.api.ai.pathfinding.PathfindingComponent;
import prisons.solar.npclib.api.ai.pathfinding.SimplePath;
import prisons.solar.npclib.api.math.Vector3d;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.world.BlockPosition;
import prisons.solar.npclib.api.world.WorldProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Goal for navigating an NPC to a target position using physics-based movement.
 * Features async pathfinding, waypoint following, and velocity-based movement.
 */
public class NavigateToPositionGoal implements Goal {
    private static final int MAX_PATH_RETRIES = 3;
    private static final long PATH_COOLDOWN_MS = 2000;
    private static final double BASE_STEP_UP_HORIZONTAL_THRESHOLD = 1.5;
    private static final long STUCK_THRESHOLD_MS = 3000; // 3 seconds
    private static final double BASE_STUCK_DISTANCE_THRESHOLD = 0.1;
    private static final double BASE_WAYPOINT_THRESHOLD = 0.5;

    private final Position target;
    private final double speed;
    private final MovementCapabilities capabilities;
    private final double arrivalThreshold;
    private final double waypointReachThreshold;  // Scaled with speed
    private final double stuckDistanceThreshold;  // Scaled with speed
    private final double jumpHorizontalThreshold; // Threshold for vertical teleport
    private final PathfindingComponent pathfinder;
    private final WorldProvider worldProvider;

    private Path currentPath;
    private int currentWaypointIndex;
    private CompletableFuture<PathResult> pendingPathRequest;
    private long pathRequestStartTime;
    private PathResult lastPathResult;
    private int consecutiveTimeouts = 0;
    private long pathCooldownUntil = 0;
    private Position lastProgressPosition;
    private long lastProgressTime;
    private boolean pathCompleted = false;  // True when path finished successfully - don't auto-repath
    private boolean isPartialPath = false;  // True if current path didn't reach actual goal
    private int stuckRepathAttempts = 0;
    private static final int MAX_STUCK_REPATH_ATTEMPTS = 3;

    public NavigateToPositionGoal(Position target, double speed, MovementCapabilities capabilities,
                                  PathfindingComponent pathfinder, WorldProvider worldProvider) {
        this.target = target;
        this.speed = speed;
        this.capabilities = capabilities;
        this.pathfinder = pathfinder;
        this.worldProvider = worldProvider;

        // Scale thresholds based on speed - faster NPCs need slightly larger reach zones
        // BUT cap aggressively to prevent skipping waypoints before jump/step-up detection
        // At speed 5.6 (sprint): threshold = 0.5, At speed 20: threshold = 1.0 (capped)
        this.waypointReachThreshold = Math.min(1.0, Math.max(BASE_WAYPOINT_THRESHOLD, speed * 0.05));
        this.stuckDistanceThreshold = Math.max(BASE_STUCK_DISTANCE_THRESHOLD, speed * 0.1);
        this.arrivalThreshold = Math.min(1.5, Math.max(0.5, speed * 0.05));
        // Jump horizontal threshold: must be LARGER than waypoint reach threshold
        // so NPC enters jump zone before marking waypoint complete
        this.jumpHorizontalThreshold = Math.max(BASE_STEP_UP_HORIZONTAL_THRESHOLD, waypointReachThreshold + 1.0);
    }

    @Override
    public boolean canStart(@NotNull NPC<?> npc) {
        if (pathfinder == null || !npc.hasPhysics()) return false;
        if (!npc.getPosition().worldId().equals(target.worldId())) return false;
        return npc.getPosition().distance(target) > arrivalThreshold;
    }

    @Override
    public void start(@NotNull NPC<?> npc) {
        currentPath = null;
        currentWaypointIndex = 0;
        lastProgressPosition = npc.getPosition();
        lastProgressTime = System.currentTimeMillis();
        pathCompleted = false;
        isPartialPath = false;
        stuckRepathAttempts = 0;
        requestPath(npc);
    }

    @Override
    public void tick(@NotNull NPC<?> npc) {
        Position current = npc.getPosition();

        // Check if arrived at final goal
        if (current.distance(target) <= arrivalThreshold) {
            stopMovement(npc);
            return;
        }

        // Handle completed path request FIRST (before checking isPendingPath)
        if (pendingPathRequest != null && pendingPathRequest.isDone()) {
            handlePathResult(npc);
        }

        // Wait if path request still in progress
        if (pendingPathRequest != null && !pendingPathRequest.isDone()) {
            return;
        }

        // Request new path if needed (with cooldown check)
        // BUT don't repath if we already completed a path successfully
        if (currentPath == null && pendingPathRequest == null && !pathCompleted) {
            if (System.currentTimeMillis() < pathCooldownUntil) {
                return; // Still in cooldown
            }
            requestPath(npc);
            return;
        }

        // Follow current path
        if (currentPath != null) {
            followPath(npc);
        }
    }

    @Override
    public void stop(@NotNull NPC<?> npc) {
        stopMovement(npc);
        currentPath = null;
        pendingPathRequest = null;
    }

    @Override
    public boolean shouldContinue(@NotNull NPC<?> npc) {
        return npc.getPosition().distance(target) > arrivalThreshold;
    }

    @Override
    public int flags() {
        return Flag.MOVE.bit | Flag.LOOK.bit;
    }

    @Override
    public int priority() {
        return -1;
    }

    private void requestPath(NPC<?> npc) {
        // Correct start and target Y positions to ensure they're on solid ground
        // This prevents A* from starting/ending at invalid Y levels when at block edges
        Position startPos = correctWaypointY(npc.getPosition());
        Position goalPos = correctWaypointY(target);

        // Dynamic timeout: longer paths get more compute time
        // Base 50ms + 5ms per block distance, capped at 200ms
        double distance = startPos.distance(goalPos);
        long timeout = Math.min(200, 50 + (long)(distance * 5));

        PathRequest request = PathRequest.builder(startPos, goalPos)
            .capabilities(capabilities)
            .maxComputeTime(timeout)
            .maxPathLength(512)
            .allowPartial(true)
            .priority(PathRequest.PathPriority.NORMAL)
            .excludeNpcId(npc.getId())
            .build();

        pathRequestStartTime = System.currentTimeMillis();
        pendingPathRequest = pathfinder.findPath(request);
    }

    private void handlePathResult(NPC<?> npc) {
        try {
            PathResult result = pendingPathRequest.get();
            lastPathResult = result;

            if (result.status() == PathResult.Status.SUCCESS || result.status() == PathResult.Status.PARTIAL) {
                // CORRECT ALL WAYPOINTS NOW (on main thread, before storing)
                Path rawPath = result.path();
                List<Position> correctedWaypoints = new ArrayList<>();
                for (Position wp : rawPath.waypoints()) {
                    correctedWaypoints.add(correctWaypointY(wp));
                }
                currentPath = new SimplePath(correctedWaypoints);
                isPartialPath = (result.status() == PathResult.Status.PARTIAL);

                currentWaypointIndex = 0;
                consecutiveTimeouts = 0;
                // Reset progress tracking timers but NOT stuckRepathAttempts
                // The stuck counter should only reset when actual movement progress is made
                // This ensures stuck NPCs eventually trigger teleport fallback
                lastProgressPosition = npc.getPosition();
                lastProgressTime = System.currentTimeMillis();
                pathCompleted = false;
            } else if (result.status() == PathResult.Status.TIMEOUT) {
                consecutiveTimeouts++;
                if (consecutiveTimeouts >= MAX_PATH_RETRIES) {
                    // Enter cooldown - stop retrying for a while
                    pathCooldownUntil = System.currentTimeMillis() + PATH_COOLDOWN_MS;
                    consecutiveTimeouts = 0;
                }
                currentPath = null;
            } else {
                // UNREACHABLE or other failure
                currentPath = null;
                consecutiveTimeouts = 0;
            }

            pendingPathRequest = null;
        } catch (Exception e) {
            // Path request failed
            pendingPathRequest = null;
            currentPath = null;
        }
    }

    private void followPath(NPC<?> npc) {
        Position current = npc.getPosition();
        List<Position> waypoints = currentPath.waypoints();

        if (isStuck(current)) {
            npc.teleport(currentPath.goal());
            return;
        }

        if (currentWaypointIndex >= waypoints.size()) {
            double distToTarget = current.distance(target);
            pathCompleted = distToTarget <= arrivalThreshold || !isPartialPath;
            stopMovement(npc);
            currentPath = null;
            stuckRepathAttempts = 0;
            return;
        }

        // Waypoints already corrected at receipt time
        Position waypoint = waypoints.get(currentWaypointIndex);

        // Waypoint reached check - threshold scales with speed for high-speed NPCs
        // IMPORTANT: For vertical transitions, require Y proximity to prevent skipping
        // jump waypoints that are within horizontal threshold but at different Y levels
        double waypointDist = current.distance(waypoint);
        double dy = Math.abs(waypoint.y() - current.y());
        double horizontalDist = Math.sqrt(
            Math.pow(waypoint.x() - current.x(), 2) + Math.pow(waypoint.z() - current.z(), 2));

        // Waypoint completion logic:
        // - Upward transitions (waypoint.y > current.y): require close proximity to ensure jump happens first
        // - Downward/same level: use speed-scaled threshold (capped at 1.0)
        boolean waypointReached;
        double waypointDy = waypoint.y() - current.y();  // Signed: positive = need to go up

        if (waypointDy > 0.1) {
            // UPWARD movement required - use strict thresholds to ensure jump/step-up triggers first
            // NPC must be very close horizontally AND have completed the vertical movement
            waypointReached = horizontalDist < 0.5 && dy < 0.3;
        } else if (dy >= 0.5) {
            // Large vertical difference (falling) - require proximity
            waypointReached = horizontalDist < 0.5 && dy < 0.5;
        } else {
            // Same level or slight downward: use speed-scaled threshold
            waypointReached = waypointDist < waypointReachThreshold;
        }

        if (waypointReached) {
            currentWaypointIndex++;
            lastProgressPosition = current;
            lastProgressTime = System.currentTimeMillis();
            stuckRepathAttempts = 0;
            if (currentWaypointIndex >= waypoints.size()) {
                double distToTarget = current.distance(target);
                pathCompleted = distToTarget <= arrivalThreshold || !isPartialPath;
                stopMovement(npc);
                currentPath = null;
            }
            return;
        }

        // Apply physics-based movement toward waypoint
        applyMovementToWaypoint(npc, current, waypoint);
    }

    /**
     * Applies movement toward a waypoint using teleport-first approach.
     * Favors teleporting for vertical transitions to avoid physics-related stuck issues.
     * Only uses physics for horizontal movement and large drops (> 1 block).
     * Handles swimming when in water.
     */
    private void applyMovementToWaypoint(NPC<?> npc, Position current, Position waypoint) {
        double dx = waypoint.x() - current.x();
        double dy = waypoint.y() - current.y();  // Positive = up, negative = down
        double dz = waypoint.z() - current.z();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Look toward waypoint while moving
        npc.lookAt(waypoint);

        // Check if we're in water (swimming)
        BlockPosition feetPos = BlockPosition.of(
            current.worldId(),
            (int) Math.floor(current.x()),
            (int) Math.floor(current.y()),
            (int) Math.floor(current.z())
        );
        boolean inWater = worldProvider.isBlockWater(feetPos);

        // Apply horizontal velocity
        double vx = 0;
        double vz = 0;
        if (horizontalDist > 0.1) {
            double moveSpeed = inWater ? speed * 0.5 : speed; // Slower in water
            vx = (dx / horizontalDist) * moveSpeed;
            vz = (dz / horizontalDist) * moveSpeed;
        }

        double vy;
        if (inWater && capabilities.canSwim()) {
            // Swimming - apply vertical velocity toward waypoint
            double swimSpeed = speed * 0.4;
            if (dy > 0.5) {
                // Need to swim up
                vy = swimSpeed;
            } else if (dy < -0.5) {
                // Need to swim down
                vy = -swimSpeed * 0.5; // Slower descent
            } else {
                // Maintain height - slight upward to counteract sinking
                vy = 0.02;
            }
            npc.setTargetVelocity(new Vector3d(vx, vy, vz));
            return; // Don't do land-based vertical transitions in water
        }

        // Land movement - preserve Y velocity for knockback/jumping
        Vector3d currentVel = npc.getVelocity();
        double preserveY = currentVel.getY();
        npc.setTargetVelocity(new Vector3d(vx, preserveY, vz));

        // Handle vertical transitions (land only)
        if (dy > 0 && dy <= capabilities.jumpHeight()) {
            double jumpThreshold = Math.max(jumpHorizontalThreshold, 2.5);
            if (horizontalDist < jumpThreshold) {
                performVerticalTeleport(npc, current, waypoint, dy);
            }
        } else if (horizontalDist < jumpHorizontalThreshold) {
            if (dy < 0 && dy >= -1.5) {
                performVerticalTeleport(npc, current, waypoint, dy);
            }
        }
    }

    /**
     * Teleports NPC to waypoint position (X, Y, Z), validating the position is safe.
     * Used for both upward (jumps/steps) and small downward (1 block drops) movement.
     * Teleports to waypoint's X/Z (block center) to prevent edge-sticking.
     * Preserves upward velocity (knockback/jump) to allow physics to continue the motion.
     */
    private void performVerticalTeleport(NPC<?> npc, Position current, Position waypoint, double dy) {
        // Teleport to waypoint's full position (block center X/Z + correct Y)
        // Using current X/Z caused NPCs to get stuck on block edges
        Position targetPos = new Position(
            waypoint.worldId(),
            waypoint.x(),  // Use waypoint X (block center)
            waypoint.y(),  // Use waypoint Y
            waypoint.z(),  // Use waypoint Z (block center)
            current.yaw(),
            current.pitch()
        );

        // Check if target position is safe (not inside solid blocks)
        if (isPositionSafe(targetPos)) {
            npc.teleport(targetPos, true);
            // Preserve upward velocity (knockback/jump), only zero if falling
            Vector3d vel = npc.getVelocity();
            if (vel.getY() < 0) {
                // Only zero Y velocity if we were falling - teleport landed us
                npc.setVelocity(new Vector3d(vel.getX(), 0, vel.getZ()));
            }
            // If vel.Y > 0 (knockback/jump), preserve it so physics continues the arc
        } else {
            // Target unsafe - try to find safe Y by scanning
            Position safePos = findSafePosition(targetPos);
            if (safePos != null) {
                npc.teleport(safePos, true);
                Vector3d vel = npc.getVelocity();
                if (vel.getY() < 0) {
                    npc.setVelocity(new Vector3d(vel.getX(), 0, vel.getZ()));
                }
            }
            // If no safe position found, let physics handle it
        }
    }

    /**
     * Checks if a position is safe (entity won't be inside solid blocks).
     */
    private boolean isPositionSafe(Position pos) {
        int blockX = (int) Math.floor(pos.x());
        int blockY = (int) Math.floor(pos.y());
        int blockZ = (int) Math.floor(pos.z());

        BlockPosition feetBlock = BlockPosition.of(pos.worldId(), blockX, blockY, blockZ);
        if (worldProvider.isBlockSolid(feetBlock)) {
            return false;
        }

        BlockPosition headBlock = BlockPosition.of(pos.worldId(), blockX, blockY + 1, blockZ);
        return !worldProvider.isBlockSolid(headBlock);
    }

    /**
     * Finds a safe position near the target by scanning up/down.
     */
    private Position findSafePosition(Position target) {
        int blockX = (int) Math.floor(target.x());
        int blockZ = (int) Math.floor(target.z());
        int baseY = (int) Math.floor(target.y());

        // Scan up to 3 blocks up and down to find safe ground
        for (int yOffset = 0; yOffset <= 3; yOffset++) {
            // Try above first
            int checkY = baseY + yOffset;
            BlockPosition ground = BlockPosition.of(target.worldId(), blockX, checkY - 1, blockZ);
            BlockPosition feet = BlockPosition.of(target.worldId(), blockX, checkY, blockZ);
            BlockPosition head = BlockPosition.of(target.worldId(), blockX, checkY + 1, blockZ);

            if (worldProvider.isBlockSolid(ground) &&
                !worldProvider.isBlockSolid(feet) &&
                !worldProvider.isBlockSolid(head)) {
                // Found safe position: solid ground, air at feet and head
                WorldProvider.BlockBoundingBox bounds = worldProvider.getBlockBounds(ground);
                double safeY = bounds != null ? bounds.maxY() : checkY;
                return new Position(target.worldId(), target.x(), safeY, target.z(),
                    target.yaw(), target.pitch());
            }

            // Try below
            if (yOffset > 0) {
                checkY = baseY - yOffset;
                ground = BlockPosition.of(target.worldId(), blockX, checkY - 1, blockZ);
                feet = BlockPosition.of(target.worldId(), blockX, checkY, blockZ);
                head = BlockPosition.of(target.worldId(), blockX, checkY + 1, blockZ);

                if (worldProvider.isBlockSolid(ground) &&
                    !worldProvider.isBlockSolid(feet) &&
                    !worldProvider.isBlockSolid(head)) {
                    WorldProvider.BlockBoundingBox bounds = worldProvider.getBlockBounds(ground);
                    double safeY = bounds != null ? bounds.maxY() : checkY;
                    return new Position(target.worldId(), target.x(), safeY, target.z(),
                        target.yaw(), target.pitch());
                }
            }
        }

        return null;  // No safe position found
    }

    /**
     * Checks if NPC is stuck (no meaningful progress for STUCK_THRESHOLD_MS).
     * Progress is measured by distance traveled since last significant movement.
     * Threshold scales with speed - fast NPCs should cover more ground.
     *
     * @param current current position
     * @return true if stuck, false otherwise
     */
    private boolean isStuck(Position current) {
        long now = System.currentTimeMillis();
        double distMoved = current.distance(lastProgressPosition);

        if (distMoved > stuckDistanceThreshold) {
            lastProgressPosition = current;
            lastProgressTime = now;
            return false;
        }

        return (now - lastProgressTime) > STUCK_THRESHOLD_MS;
    }

    /**
     * Corrects waypoint Y coordinate for partial blocks (slabs, stairs, dirt paths).
     * Pathfinding uses integer Y, but partial blocks have fractional heights.
     * Called on main thread where WorldProvider access is safe.
     * Checks ALL blocks the entity's hitbox would touch to handle edge straddling.
     *
     * @param waypoint the raw waypoint from pathfinding
     * @return waypoint with corrected Y based on ground block height
     */
    private Position correctWaypointY(Position waypoint) {
        // Check ALL blocks the entity's 0.6-width hitbox would touch
        int minX = (int) Math.floor(waypoint.x() - 0.3);
        int maxX = (int) Math.floor(waypoint.x() + 0.3);
        int minZ = (int) Math.floor(waypoint.z() - 0.3);
        int maxZ = (int) Math.floor(waypoint.z() + 0.3);
        int baseY = (int) Math.floor(waypoint.y());

        // Scan 10 blocks down (increased from 5) to find actual ground
        for (int yOffset = 0; yOffset >= -10; yOffset--) {
            int checkY = baseY + yOffset;
            double highestY = Double.MIN_VALUE;

            // Find the highest solid block at this Y level across all hitbox positions
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPosition checkBlock = BlockPosition.of(waypoint.worldId(), x, checkY, z);
                    WorldProvider.BlockBoundingBox bounds = worldProvider.getBlockBounds(checkBlock);
                    if (bounds != null && bounds.solid()) {
                        highestY = Math.max(highestY, bounds.maxY());
                    }
                }
            }

            if (highestY > Double.MIN_VALUE) {
                return new Position(
                    waypoint.worldId(),
                    waypoint.x(),
                    highestY,
                    waypoint.z(),
                    waypoint.yaw(),
                    waypoint.pitch()
                );
            }
        }

        return waypoint;
    }

    /**
     * Stops horizontal movement, clearing target velocity and preserving vertical for landing.
     */
    private void stopMovement(NPC<?> npc) {
        // Clear target velocity so friction can naturally stop the NPC
        npc.setTargetVelocity(null);
        // Also zero out current horizontal velocity
        Vector3d currentVel = npc.getVelocity();
        npc.setVelocity(new Vector3d(0, currentVel.getY(), 0));
    }

    public Path getCurrentPath() { return currentPath; }
    public int getCurrentWaypointIndex() { return currentWaypointIndex; }
    public boolean isPendingPath() { return pendingPathRequest != null && !pendingPathRequest.isDone(); }
    public long getPathRequestStartTime() { return pathRequestStartTime; }
    public PathResult getLastPathResult() { return lastPathResult; }

    /**
     * Returns true if navigation has finished (path completed or failed) and no new path is pending.
     * Used by parent goals to detect when they need to handle unreachable targets.
     */
    public boolean isNavigationFinished() {
        return currentPath == null && pendingPathRequest == null;
    }

    /**
     * Returns true if the last path attempt resulted in PARTIAL (goal unreachable).
     */
    public boolean wasPartialPath() {
        return isPartialPath;
    }
}
