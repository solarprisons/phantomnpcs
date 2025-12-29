package prisons.solar.npclib.core.physics;

import prisons.solar.npclib.api.math.Vector3d;
import prisons.solar.npclib.api.npc.NPC;

public class PhysicsState {
    private final NPC<?> npc;
    private volatile Vector3d velocity = Vector3d.ZERO;
    private volatile Vector3d targetVelocity = null; // Target velocity to maintain (e.g., for continuous movement)
    private boolean gravityEnabled = true;
    private float gravityMultiplier = 1.0f;
    private volatile boolean onGround = false;

    /**
     * If true, physics will only run when at least one viewer can see this NPC.
     * When no viewers are present, the NPC will be snapped to the ground to prevent floating.
     * Default: true (physics requires viewers for optimal performance)
     */
    private boolean requiresViewersForPhysics = true;

    /**
     * The last position where physics was actually computed.
     * Used for movement threshold optimization.
     */
    private double lastPhysicsX = 0;
    private double lastPhysicsY = 0;
    private double lastPhysicsZ = 0;
    private boolean hasLastPhysicsPosition = false;

    /**
     * Movement threshold in blocks squared.
     * Physics is skipped if the NPC hasn't moved this far since last physics tick.
     * Default: 0.001² = very small movement (0.03 blocks)
     */
    private static final double MOVEMENT_THRESHOLD_SQ = 0.001 * 0.001;

    public PhysicsState(NPC<?> npc) {
        this.npc = npc;
    }

    public NPC<?> getNpc() {
        return npc;
    }

    public Vector3d getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector3d velocity) {
        this.velocity = velocity;
    }

    public boolean isGravityEnabled() {
        return gravityEnabled;
    }

    public void setGravityEnabled(boolean gravityEnabled) {
        this.gravityEnabled = gravityEnabled;
    }

    public float getGravityMultiplier() {
        return gravityMultiplier;
    }

    public void setGravityMultiplier(float gravityMultiplier) {
        this.gravityMultiplier = gravityMultiplier;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }

    public Vector3d getTargetVelocity() {
        return targetVelocity;
    }

    public void setTargetVelocity(Vector3d targetVelocity) {
        this.targetVelocity = targetVelocity;
    }

    /**
     * Gets whether this NPC requires viewers for physics to run.
     *
     * @return true if physics only runs when viewers are present
     */
    public boolean requiresViewersForPhysics() {
        return requiresViewersForPhysics;
    }

    /**
     * Sets whether this NPC requires viewers for physics to run.
     * If true and no viewers are present, physics is skipped and the NPC is snapped to ground.
     *
     * @param requiresViewersForPhysics true to require viewers for physics
     */
    public void setRequiresViewersForPhysics(boolean requiresViewersForPhysics) {
        this.requiresViewersForPhysics = requiresViewersForPhysics;
    }

    /**
     * Checks if physics should run based on movement threshold.
     * Skips physics if the NPC hasn't moved significantly since last physics tick.
     *
     * @param currentX current X position
     * @param currentY current Y position
     * @param currentZ current Z position
     * @return true if physics should run
     */
    public boolean needsPhysicsTick(double currentX, double currentY, double currentZ) {
        if (!hasLastPhysicsPosition) {
            return true; // First tick always needs physics
        }

        // Calculate distance squared from last physics position
        double dx = currentX - lastPhysicsX;
        double dy = currentY - lastPhysicsY;
        double dz = currentZ - lastPhysicsZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        return distSq > MOVEMENT_THRESHOLD_SQ || !onGround || velocity.lengthSquared() > 0.01;
    }

    /**
     * Updates the last physics position.
     * Call this after physics has been computed for this tick.
     *
     * @param x the X position
     * @param y the Y position
     * @param z the Z position
     */
    public void updateLastPhysicsPosition(double x, double y, double z) {
        this.lastPhysicsX = x;
        this.lastPhysicsY = y;
        this.lastPhysicsZ = z;
        this.hasLastPhysicsPosition = true;
    }
}
