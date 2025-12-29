package prisons.solar.npclib.core.ai.goals;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import prisons.solar.npclib.api.ai.Goal;
import prisons.solar.npclib.api.ai.pathfinding.MovementCapabilities;
import prisons.solar.npclib.api.ai.pathfinding.PathfindingComponent;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.world.BlockPosition;
import prisons.solar.npclib.api.world.WorldProvider;

import java.util.function.Supplier;

/**
 * Goal that follows a dynamic target position.
 *
 * <p>Uses Supplier pattern for flexible target resolution - the platform adapter
 * is responsible for converting platform-specific entities to Position.
 *
 * <p>Example usage from Paper:
 * <pre>{@code
 * Player target = ...;
 * Supplier<Position> positionSupplier = () -> {
 *     if (target.isDead()) return null;
 *     Location loc = target.getLocation();
 *     return Position.of(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
 * };
 * npc.getGoalSelector().addGoal(new FollowTargetGoal(..., positionSupplier, ...));
 * }</pre>
 */
public final class FollowTargetGoal implements Goal {

    // Dependencies
    private final PathfindingComponent pathfinder;
    private final WorldProvider worldProvider;
    private final Supplier<Position> targetPositionSupplier;

    // Configuration
    private final double stopDistance;      // Stop when this close to target
    private final double followDistance;    // Resume following when target is this far
    private final double maxRange;          // Give up if target exceeds this range
    private final MovementCapabilities capabilities;
    private final double speed;
    private final boolean mimicVerticalMovement;  // Teleport to airborne targets (Cosmic-style)

    // Constants
    private static final long REPATH_COOLDOWN_MS = 500;
    private static final double REPATH_DISTANCE_SQ = 4.0; // 2 blocks squared

    // Runtime state - delegates actual navigation to NavigateToPositionGoal
    private NavigateToPositionGoal navigationGoal;
    private Position lastTargetPosition;
    private long lastRepathTime;

    /**
     * Creates a new FollowTargetGoal.
     *
     * @param pathfinder            pathfinding component
     * @param worldProvider         world provider for block queries
     * @param targetPositionSupplier supplies target position (returns null if invalid)
     * @param stopDistance          stop moving when this close
     * @param followDistance        resume following when target moves this far
     * @param maxRange              give up if target exceeds this distance
     * @param capabilities          movement capabilities
     * @param speed                 movement speed
     * @param mimicVerticalMovement if true, teleport to airborne targets (Cosmic-style guards)
     */
    public FollowTargetGoal(
            @NotNull PathfindingComponent pathfinder,
            @NotNull WorldProvider worldProvider,
            @NotNull Supplier<Position> targetPositionSupplier,
            double stopDistance,
            double followDistance,
            double maxRange,
            @NotNull MovementCapabilities capabilities,
            double speed,
            boolean mimicVerticalMovement
    ) {
        this.pathfinder = pathfinder;
        this.worldProvider = worldProvider;
        this.targetPositionSupplier = targetPositionSupplier;
        this.stopDistance = stopDistance;
        this.followDistance = followDistance;
        this.maxRange = maxRange;
        this.capabilities = capabilities;
        this.speed = speed;
        this.mimicVerticalMovement = mimicVerticalMovement;
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public int flags() {
        return Flag.MOVE.bit | Flag.LOOK.bit;
    }

    @Override
    public boolean canStart(@NotNull NPC<?> npc) {
        if (!npc.hasPhysics()) {
            return false;
        }

        Position targetPos = targetPositionSupplier.get();
        if (targetPos == null ||
                !targetPos.worldId().equals(npc.getPosition().worldId()) ||
                targetPos.equals(npc.getPosition())) {
            return false;
        }

        double distSq = npc.getPosition().distanceSquared(targetPos);
        return distSq > followDistance * followDistance
            && distSq < maxRange * maxRange;
    }

    @Override
    public boolean shouldContinue(@NotNull NPC<?> npc) {
        Position targetPos = targetPositionSupplier.get();
        if (targetPos == null || !targetPos.worldId().equals(npc.getPosition().worldId())) {
            return false;
        }

        double distSq = npc.getPosition().distanceSquared(targetPos);
        return distSq < maxRange * maxRange;
    }

    @Override
    public void start(@NotNull NPC<?> npc) {
        lastTargetPosition = null;
        lastRepathTime = 0;
        navigationGoal = null;
    }

    @Override
    public void stop(@NotNull NPC<?> npc) {
        if (navigationGoal != null) {
            navigationGoal.stop(npc);
            navigationGoal = null;
        }
        lastTargetPosition = null;
    }

    @Override
    public void tick(@NotNull NPC<?> npc) {
        Position targetPos = targetPositionSupplier.get();
        if (targetPos == null) {
            return;
        }

        Position npcPos = npc.getPosition();
        double distSq = npcPos.distanceSquared(targetPos);

        // Check if we're close enough to target (3D distance)
        if (distSq <= stopDistance * stopDistance) {
            if (navigationGoal != null) {
                navigationGoal.stop(npc);
            }
            npc.lookAt(targetPos);
            return;
        }

        // Normal repath check (target moved significantly)
        if (shouldRepath(targetPos)) {
            if (navigationGoal != null) {
                navigationGoal.stop(npc);
            }
            navigationGoal = createNavigationGoal(targetPos);
            navigationGoal.start(npc);
        }

        // Navigation finished but still far - teleport if allowed
        if (navigationGoal != null && navigationGoal.isNavigationFinished()) {
            if (mimicVerticalMovement || isPositionGrounded(targetPos)) {
                npc.teleport(targetPos);
                navigationGoal = createNavigationGoal(targetPos);
                navigationGoal.start(npc);
            }
            return;
        }

        if (navigationGoal == null) return;

        navigationGoal.tick(npc);
    }

    // ==================== Helper Methods ====================

    /**
     * Checks if we should request a new path to the target.
     * Returns true if target moved significantly AND cooldown elapsed.
     */
    private boolean shouldRepath(Position targetPos) {
        long now = System.currentTimeMillis();
        if (now - lastRepathTime < REPATH_COOLDOWN_MS) {
            return false;
        }
        if (lastTargetPosition == null) {
            return true;
        }
        return targetPos.distanceSquared(lastTargetPosition) > REPATH_DISTANCE_SQ;
    }

    /**
     * Creates a new NavigateToPositionGoal to the target position.
     * Also updates tracking state.
     */
    private NavigateToPositionGoal createNavigationGoal(Position targetPos) {
        lastTargetPosition = targetPos;
        lastRepathTime = System.currentTimeMillis();
        return new NavigateToPositionGoal(targetPos, speed, capabilities, pathfinder, worldProvider);
    }

    /**
     * Checks if a position has solid ground below it (entity is grounded).
     * Returns false for airborne positions (mid-jump, falling).
     */
    private boolean isPositionGrounded(Position pos) {
        int blockX = (int) Math.floor(pos.x());
        int blockY = (int) Math.floor(pos.y());
        int blockZ = (int) Math.floor(pos.z());

        BlockPosition below = BlockPosition.of(pos.worldId(), blockX, blockY - 1, blockZ);
        return worldProvider.isBlockSolid(below);
    }
}
