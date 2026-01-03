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

    // Fire state - vanilla: 160 ticks (8 seconds) default burn time
    private int fireTicks = 0;
    private int fireTicksOnFireDamage = 0; // Counter for fire damage timing (every 20 ticks = 1 second)
    private boolean fireImmune = false;

    // Water/liquid state
    private boolean inWater = false;
    private boolean inLava = false;
    private int airSupply = 300; // Vanilla: 300 ticks (15 seconds) max air
    private static final int MAX_AIR_SUPPLY = 300;
    private int drownDamageTimer = 0; // Ticks until next drown damage

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

    // Fire state methods

    /**
     * Gets remaining fire ticks. When > 0, entity is visually on fire.
     *
     * @return remaining fire ticks
     */
    public int getFireTicks() {
        return fireTicks;
    }

    /**
     * Sets fire ticks. Vanilla default is 160 (8 seconds).
     *
     * @param ticks fire ticks to set
     */
    public void setFireTicks(int ticks) {
        this.fireTicks = Math.max(0, ticks);
    }

    /**
     * Checks if entity is currently on fire (has fire ticks).
     *
     * @return true if on fire
     */
    public boolean isOnFire() {
        return fireTicks > 0;
    }

    /**
     * Gets the fire damage timer (ticks since last fire damage).
     *
     * @return fire damage timer
     */
    public int getFireDamageTimer() {
        return fireTicksOnFireDamage;
    }

    /**
     * Sets the fire damage timer.
     *
     * @param timer new timer value
     */
    public void setFireDamageTimer(int timer) {
        this.fireTicksOnFireDamage = timer;
    }

    /**
     * Checks if entity is immune to fire damage.
     *
     * @return true if fire immune
     */
    public boolean isFireImmune() {
        return fireImmune;
    }

    /**
     * Sets fire immunity.
     *
     * @param fireImmune true to make fire immune
     */
    public void setFireImmune(boolean fireImmune) {
        this.fireImmune = fireImmune;
    }

    /**
     * Extinguishes fire (sets fire ticks to 0).
     */
    public void extinguish() {
        this.fireTicks = 0;
        this.fireTicksOnFireDamage = 0;
    }

    // Water/liquid state methods

    /**
     * Checks if entity is in water.
     *
     * @return true if in water
     */
    public boolean isInWater() {
        return inWater;
    }

    /**
     * Sets whether entity is in water.
     *
     * @param inWater true if in water
     */
    public void setInWater(boolean inWater) {
        this.inWater = inWater;
    }

    /**
     * Checks if entity is in lava.
     *
     * @return true if in lava
     */
    public boolean isInLava() {
        return inLava;
    }

    /**
     * Sets whether entity is in lava.
     *
     * @param inLava true if in lava
     */
    public void setInLava(boolean inLava) {
        this.inLava = inLava;
    }

    /**
     * Gets current air supply (ticks of air remaining).
     * Vanilla max is 300 ticks (15 seconds).
     *
     * @return air supply ticks
     */
    public int getAirSupply() {
        return airSupply;
    }

    /**
     * Sets air supply.
     *
     * @param airSupply air supply ticks
     */
    public void setAirSupply(int airSupply) {
        this.airSupply = Math.max(0, Math.min(airSupply, MAX_AIR_SUPPLY));
    }

    /**
     * Gets maximum air supply.
     *
     * @return max air supply (300 ticks)
     */
    public int getMaxAirSupply() {
        return MAX_AIR_SUPPLY;
    }

    /**
     * Restores air supply to maximum.
     */
    public void restoreAirSupply() {
        this.airSupply = MAX_AIR_SUPPLY;
        this.drownDamageTimer = 0;
    }

    /**
     * Gets the drown damage timer.
     *
     * @return drown damage timer
     */
    public int getDrownDamageTimer() {
        return drownDamageTimer;
    }

    /**
     * Sets the drown damage timer.
     *
     * @param timer new timer value
     */
    public void setDrownDamageTimer(int timer) {
        this.drownDamageTimer = timer;
    }

    /**
     * Checks if entity is submerged in any liquid.
     *
     * @return true if in water or lava
     */
    public boolean isInLiquid() {
        return inWater || inLava;
    }

    // Swimming pose state - tracked to avoid recalculating every tick
    private boolean activelySwimming = false;

    /**
     * Gets whether the entity is actively swimming (moving in water).
     * Used to determine SWIMMING pose.
     *
     * @return true if actively swimming
     */
    public boolean isActivelySwimming() {
        return activelySwimming;
    }

    /**
     * Sets the actively swimming state.
     *
     * @param activelySwimming true if actively swimming
     * @return true if the state changed
     */
    public boolean setActivelySwimming(boolean activelySwimming) {
        if (this.activelySwimming != activelySwimming) {
            this.activelySwimming = activelySwimming;
            return true;
        }
        return false;
    }
}
