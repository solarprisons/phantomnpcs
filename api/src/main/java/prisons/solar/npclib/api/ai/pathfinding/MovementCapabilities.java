package prisons.solar.npclib.api.ai.pathfinding;

/**
 * Defines movement capabilities for NPC pathfinding and navigation.
 *
 * @param jumpHeight     Maximum jump height in blocks
 * @param fallTolerance  Maximum safe fall distance in blocks
 * @param canSwim        Whether the entity can swim
 * @param canWalkOnLava  Whether the entity can walk on lava
 * @param entityWidth    Entity hitbox width (diameter)
 * @param entityHeight   Entity hitbox height
 * @param stepHeight     Maximum height that can be stepped up without jumping
 * @param walkSpeed      Walking speed in blocks per second
 * @param sprintSpeed    Sprinting speed in blocks per second
 */
public record MovementCapabilities(
        double jumpHeight,
        double fallTolerance,
        boolean canSwim,
        boolean canWalkOnLava,
        double entityWidth,
        double entityHeight,
        double stepHeight,
        double walkSpeed,
        double sprintSpeed
) {
    /** Default player walking speed: 4.317 blocks/second */
    public static final double DEFAULT_WALK_SPEED = 4.317d;

    /** Default player sprinting speed: 5.612 blocks/second (1.3x walk) */
    public static final double DEFAULT_SPRINT_SPEED = 5.612d;

    public static final MovementCapabilities DEFAULT =
            new MovementCapabilities(
                    1.0d,
                    3.0d,
                    true,
                    false,
                    0.6d,
                    1.8d,
                    1.0d,
                    DEFAULT_WALK_SPEED,
                    DEFAULT_SPRINT_SPEED);

    /**
     * Creates capabilities with custom speeds.
     */
    public MovementCapabilities withSpeeds(double walkSpeed, double sprintSpeed) {
        return new MovementCapabilities(
                jumpHeight, fallTolerance, canSwim, canWalkOnLava,
                entityWidth, entityHeight, stepHeight, walkSpeed, sprintSpeed);
    }

    /**
     * Creates capabilities with a speed multiplier applied to both walk and sprint.
     */
    public MovementCapabilities withSpeedMultiplier(double multiplier) {
        return new MovementCapabilities(
                jumpHeight, fallTolerance, canSwim, canWalkOnLava,
                entityWidth, entityHeight, stepHeight,
                walkSpeed * multiplier, sprintSpeed * multiplier);
    }
}
