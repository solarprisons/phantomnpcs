package prisons.solar.npclib.core.ai.pathfinding;

import prisons.solar.npclib.api.ai.pathfinding.MovementCapabilities;
import prisons.solar.npclib.api.world.BlockPosition;
import prisons.solar.npclib.api.world.WorldProvider;

/**
 * Validates NPC movement from one position to another.
 * Checks destination walkability, head clearance, ground support, and fall distance.
 * Core component for pathfinding - determines which moves are valid and their costs.
 */
public final class MovementValidator {
    private final WorldProvider worldProvider;

    public MovementValidator(WorldProvider worldProvider) {
        this.worldProvider = worldProvider;
    }


    /**
     * Validates if an NPC can move from one position to another.
     *
     * @param from starting position
     * @param to destination position
     * @param caps movement capabilities
     * @return move result with type, cost, and blocked reason
     */
    public MoveResult validateMove(BlockPosition from, BlockPosition to, MovementCapabilities caps) {
        if (worldProvider.isBlockSolid(to)) {
            return new MoveResult(MoveType.BLOCKED, 0, "Destination is solid");
        }

        // Check head clearance using actual block bounds (handles slabs, stairs)
        double entityHeight = caps.entityHeight();
        double npcTopY = to.y() + entityHeight;  // Top of NPC's head

        int headClearanceBlocks = (int) Math.ceil(entityHeight);
        for (int y = 1; y <= headClearanceBlocks; y++) {
            BlockPosition headBlock = to.add(0, y, 0);
            WorldProvider.BlockBoundingBox bounds = worldProvider.getBlockBounds(headBlock);
            if (bounds != null && bounds.solid()) {
                // Check if NPC head actually intersects with block's collision box
                // For bottom slabs at y+1, their minY is at y+1.0, NPC head at y+1.8 won't collide
                double blockMinY = bounds.minY();
                if (npcTopY > blockMinY) {
                    return new MoveResult(MoveType.BLOCKED, 0, "Insufficient head clearance");
                }
            }
        }

        BlockPosition below = to.add(0, -1, 0);
        boolean hasGroundSupport = worldProvider.isBlockSolid(below);

        int dy = to.y() - from.y();
        int dx = to.x() - from.x();
        int dz = to.z() - from.z();
        boolean isDiagonal = dx != 0 && dz != 0;

        // Check diagonal corner blocking - can't squeeze through two adjacent solid blocks
        if (isDiagonal) {
            // Check at destination Y level (handles both walks and jumps)
            BlockPosition sideX = BlockPosition.of(from.worldId(), to.x(), to.y(), from.z());
            BlockPosition sideZ = BlockPosition.of(from.worldId(), from.x(), to.y(), to.z());
            if (worldProvider.isBlockSolid(sideX) && worldProvider.isBlockSolid(sideZ)) {
                return new MoveResult(MoveType.BLOCKED, 0, "Diagonal blocked by corner");
            }
            // Also check head level for tall entities
            BlockPosition sideXHead = sideX.add(0, 1, 0);
            BlockPosition sideZHead = sideZ.add(0, 1, 0);
            if (worldProvider.isBlockSolid(sideXHead) && worldProvider.isBlockSolid(sideZHead)) {
                return new MoveResult(MoveType.BLOCKED, 0, "Diagonal blocked by corner at head level");
            }
        }

        // For jumps (dy > 0), REQUIRE solid ground to land on - no jumping to air
        if (dy > 0 && !hasGroundSupport) {
            return new MoveResult(MoveType.BLOCKED, 0, "No ground to land on after jump");
        }

        // For jumps, check head clearance ABOVE the starting position (jump arc)
        if (dy > 0) {
            int jumpClearanceStart = (int) Math.ceil(entityHeight);
            int jumpClearanceEnd = jumpClearanceStart + dy;
            for (int y = jumpClearanceStart; y <= jumpClearanceEnd; y++) {
                BlockPosition jumpBlock = from.add(0, y, 0);
                if (worldProvider.isBlockSolid(jumpBlock)) {
                    return new MoveResult(MoveType.BLOCKED, 0, "No head clearance for jump at Y+" + y);
                }
            }
        }

        // For walks and falls, calculate fall distance once and validate
        int implicitFallDistance = 0;
        if (!hasGroundSupport) {
            implicitFallDistance = calculateFallDistance(to, (int) caps.fallTolerance() + 1);
            if (implicitFallDistance > caps.fallTolerance()) {
                return new MoveResult(MoveType.BLOCKED, 0, "Excessive fall distance: " + implicitFallDistance);
            }
        }

        double baseCost = isDiagonal ? 1.414 : 1.0;

        if (dy == 0) {
            // Walking into a hole - apply fall cost even though dy == 0
            if (!hasGroundSupport) {
                double fallCost = baseCost + (3.0 * implicitFallDistance);
                return new MoveResult(MoveType.FALL, fallCost, null);
            }
            return new MoveResult(MoveType.WALK, baseCost, null);
        } else if (dy > 0) {
            if (dy > caps.jumpHeight()) {
                return new MoveResult(MoveType.BLOCKED, 0, "Jump too high: " + dy + " > " + caps.jumpHeight());
            }
            double jumpCost = baseCost + (2.0 * dy);
            return new MoveResult(MoveType.JUMP, jumpCost, null);
        } else {
            int fallDistance = -dy;
            if (fallDistance > caps.fallTolerance()) {
                return new MoveResult(MoveType.BLOCKED, 0, "Fall too far: " + fallDistance + " > " + caps.fallTolerance());
            }
            // High fall cost discourages pathfinder from choosing routes that drop into holes
            // Cost: base + 3.0 per block fallen (much higher than walking/jumping)
            double fallCost = baseCost + (3.0 * fallDistance);
            return new MoveResult(MoveType.FALL, fallCost, null);
        }
    }

    /**
     * Calculates fall distance from a position by scanning downward until hitting solid ground.
     *
     * @param from starting position
     * @param maxDistance maximum distance to scan
     * @return fall distance in blocks, or maxDistance if no ground found
     */
    private int calculateFallDistance(BlockPosition from, int maxDistance) {
        for (int i = 1; i <= maxDistance; i++) {
            BlockPosition check = from.add(0, -i, 0);
            if (worldProvider.isBlockSolid(check)) {
                return i - 1;
            }
        }
        return maxDistance;
    }

    public enum MoveType {
        WALK,
        JUMP,
        FALL,
        BLOCKED
    }

    /**
     * Result of movement validation.
     *
     * @param type movement type (WALK, JUMP, FALL, or BLOCKED)
     * @param cost movement cost (0 if blocked)
     * @param blockedReason reason for blocked movement (null if not blocked)
     */
    public record MoveResult(MoveType type, double cost, String blockedReason) {
        public boolean isBlocked() {
            return type == MoveType.BLOCKED;
        }
    }
}
