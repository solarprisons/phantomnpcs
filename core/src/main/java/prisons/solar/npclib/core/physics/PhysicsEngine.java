package prisons.solar.npclib.core.physics;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.math.Vector3d;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.world.BlockPosition;
import prisons.solar.npclib.api.world.WorldProvider;
import prisons.solar.npclib.core.pool.ObjectPoolManager;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PhysicsEngine {
    private final Map<UUID, PhysicsState> entities = new ConcurrentHashMap<>();
    private final CollisionManager collisionManager;
    private final ScheduledExecutorService scheduler;

    private volatile long totalTicks = 0;
    private volatile long totalTickTimeNanos = 0;
    private volatile long slowestTickNanos = 0;
    private volatile long lastTickTimeNanos = 0;
    private volatile long entitiesSkippedNoViewers = 0;
    private volatile long entitiesSkippedMovementThreshold = 0;

    public Map<UUID, PhysicsState> getEntities() {
        return entities;
    }

    public CollisionManager getCollisionManager() {
        return collisionManager;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    /**
     * Gets the total number of physics ticks executed.
     *
     * @return total tick count
     */
    public long getTotalTicks() {
        return totalTicks;
    }

    /**
     * Gets the average tick time in milliseconds.
     *
     * @return average tick time in ms, or 0 if no ticks have executed
     */
    public double getAverageTickTime() {
        return totalTicks > 0 ? (totalTickTimeNanos / (double) totalTicks) / 1_000_000.0 : 0.0;
    }

    /**
     * Gets the slowest tick time in milliseconds.
     *
     * @return slowest tick time in ms
     */
    public double getSlowestTickTime() {
        return slowestTickNanos / 1_000_000.0;
    }

    /**
     * Gets the last tick time in milliseconds.
     *
     * @return last tick time in ms
     */
    public double getLastTickTime() {
        return lastTickTimeNanos / 1_000_000.0;
    }

    /**
     * Resets all performance metrics.
     */
    public void resetMetrics() {
        totalTicks = 0;
        totalTickTimeNanos = 0;
        slowestTickNanos = 0;
        lastTickTimeNanos = 0;
        entitiesSkippedNoViewers = 0;
        entitiesSkippedMovementThreshold = 0;
        ObjectPoolManager.getInstance().resetAllMetrics();
    }

    /**
     * Gets the total number of entities skipped due to no viewers.
     *
     * @return entities skipped count
     */
    public long getEntitiesSkippedNoViewers() {
        return entitiesSkippedNoViewers;
    }

    /**
     * Gets the total number of entities skipped due to movement threshold.
     *
     * @return entities skipped count
     */
    public long getEntitiesSkippedMovementThreshold() {
        return entitiesSkippedMovementThreshold;
    }

    /**
     * Gets the object pool statistics for monitoring allocation efficiency.
     *
     * @return pool statistics
     */
    public ObjectPoolManager.PoolStatistics getPoolStatistics() {
        return ObjectPoolManager.getInstance().getStatistics();
    }

    private static final double GRAVITY = -32.0d;
    private static final double TERMINAL_VELOCITY = -78.4d;
    private static final double FRICTION = 0.6d;
    private static final double AIR_RESISTANCE = 0.98d;
    private static final double VELOCITY_THRESHOLD = 0.1d;
    private static final double MAX_MOVEMENT_PER_STEP = 0.8d;  // Prevents tunneling through blocks

    // Vanilla water physics constants
    private static final double WATER_GRAVITY = -0.02d;  // Very slow downward pull in water
    private static final double WATER_DRAG = 0.8d;  // Horizontal drag in water
    private static final double WATER_VERTICAL_DRAG = 0.8d;  // Vertical drag slows falling
    private static final double WATER_TERMINAL_VELOCITY = -0.4d;  // Max sink speed in water (much slower than air)
    private static final double BUBBLE_COLUMN_UP_SPEED = 0.7d;  // Soul sand bubble column
    private static final double BUBBLE_COLUMN_DOWN_SPEED = 0.9d;  // Magma bubble column

    // Vanilla lava physics constants
    private static final double LAVA_GRAVITY = -8.0d;  // Much slower fall in lava
    private static final double LAVA_DRAG = 0.5d;  // Heavy drag in lava
    private static final int LAVA_FIRE_TICKS = 300;  // 15 seconds of fire from lava

    // Vanilla fire constants
    private static final int FIRE_BLOCK_TICKS = 160;  // 8 seconds from fire blocks
    private static final int FIRE_DAMAGE_INTERVAL = 20;  // Damage every second (20 ticks)
    private static final float FIRE_DAMAGE = 1.0f;  // 0.5 hearts per tick
    private static final float LAVA_DAMAGE = 4.0f;  // 2 hearts per tick

    // Vanilla drowning constants
    private static final int DROWN_DAMAGE_INTERVAL = 20;  // Damage every second when out of air
    private static final float DROWN_DAMAGE = 2.0f;  // 1 heart per tick
    private static final int AIR_RECOVERY_RATE = 4;  // Air recovered per tick out of water

    public PhysicsEngine(CollisionManager collisionManager, ScheduledExecutorService scheduler) {
        this.collisionManager = collisionManager;
        this.scheduler = scheduler;

        getScheduler().scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
    }


    public void registerEntity(NPC<?> npc) {
        PhysicsState state = new PhysicsState(npc);
        entities.put(npc.getId(), state);
    }

    public void unregisterEntity(NPC<?> npc){
        entities.remove(npc.getId());
    }

    private void tick() {
        long startTime = System.nanoTime();

        try {
            float deltaTime = 0.05f;

            for (PhysicsState state : entities.values()) {
                tickEntity(state, deltaTime);
            }
        } finally {
            ObjectPoolManager.getInstance().resetAll();

            long tickTime = System.nanoTime() - startTime;
            totalTicks++;
            totalTickTimeNanos += tickTime;
            lastTickTimeNanos = tickTime;

            if (tickTime > slowestTickNanos) {
                slowestTickNanos = tickTime;
            }

        }
    }

    private void tickEntity(PhysicsState state, float deltaTime) {
        NPC<?> npc = state.getNpc();

        if (!state.isGravityEnabled()) return;

        Position currentPos = npc.getPosition();

        // Process environmental physics (fire, water, lava)
        processEnvironmentalPhysics(state, currentPos);

        if (state.isOnGround()) {
            BlockPosition below = BlockPosition.of(
                currentPos.worldId(),
                (int) Math.floor(currentPos.x()),
                (int) Math.floor(currentPos.y() - 0.1), // Slightly below feet
                (int) Math.floor(currentPos.z())
            );

            WorldProvider worldProvider = collisionManager.getWorldProvider();
            boolean blockBelowExists = worldProvider.isBlockSolid(below);

            if (!blockBelowExists) {
                state.setOnGround(false);
                Vector3d currentVel = state.getVelocity();
                state.setVelocity(new Vector3d(currentVel.getX(), -0.1, currentVel.getZ()));
            }
        }

        if (state.requiresViewersForPhysics() && npc.viewers().isEmpty()) {
            entitiesSkippedNoViewers++;
            return;
        }

        if (!state.needsPhysicsTick(currentPos.x(), currentPos.y(), currentPos.z())) {
            entitiesSkippedMovementThreshold++;
            return;
        }

        Vector3d v = getVector3d(state, deltaTime, npc);

        if (!isValidVector(v)) {
            v = Vector3d.ZERO;
            state.setVelocity(v);
            return;
        }

        Vector3d movement = v.multiply(deltaTime);
        double movementMagnitude = movement.length();
        Vector3d totalAdjustedMovement;
        boolean hitGround = false;

        if (movementMagnitude > MAX_MOVEMENT_PER_STEP) {
            int steps = (int) Math.ceil(movementMagnitude / MAX_MOVEMENT_PER_STEP);
            Vector3d stepMovement = new Vector3d(
                movement.getX() / steps,
                movement.getY() / steps,
                movement.getZ() / steps
            );

            double totalX = 0, totalY = 0, totalZ = 0;

            for (int i = 0; i < steps; i++) {
                Vector3d adjusted = collisionManager.resolveCollision(npc, stepMovement);
                totalX += adjusted.getX();
                totalY += adjusted.getY();
                totalZ += adjusted.getZ();

                if (stepMovement.getY() < 0 && adjusted.getY() == 0) {
                    hitGround = true;
                    break;
                }

                Position stepPos = new Position(
                    currentPos.worldId(),
                    currentPos.x() + totalX,
                    currentPos.y() + totalY,
                    currentPos.z() + totalZ,
                    currentPos.yaw(),
                    currentPos.pitch()
                );
                collisionManager.updatePosition(npc, stepPos);
            }

            totalAdjustedMovement = new Vector3d(totalX, totalY, totalZ);
        } else {
            totalAdjustedMovement = collisionManager.resolveCollision(npc, movement);
            hitGround = movement.getY() < 0 && totalAdjustedMovement.getY() == 0;
        }

        if (hitGround) {
            state.setOnGround(true);
            v = new Vector3d(v.getX(), 0, v.getZ());
        } else {
            state.setOnGround(false);
        }

        state.setVelocity(v);

        if (totalAdjustedMovement.lengthSquared() > 0.0001) {
            Position newPos = new Position(
                    currentPos.worldId(),
                    currentPos.x() + totalAdjustedMovement.getX(),
                    currentPos.y() + totalAdjustedMovement.getY(),
                    currentPos.z() + totalAdjustedMovement.getZ(),
                    currentPos.yaw(),
                    currentPos.pitch()
            );

            npc.teleport(newPos, true);
            collisionManager.updatePosition(npc, newPos);
            state.updateLastPhysicsPosition(newPos.x(), newPos.y(), newPos.z());
        } else {
            state.updateLastPhysicsPosition(currentPos.x(), currentPos.y(), currentPos.z());
        }
    }

    public void applyImpulse(NPC<?> npc, Vector3d impulse){
        PhysicsState state = entities.get(npc.getId());
        if (state != null) state.setVelocity(state.getVelocity().add(impulse));
    }

    public Optional<Vector3d> getVelocity(NPC<?> npc) {
        PhysicsState state = entities.get(npc.getId());
        return Optional.ofNullable(state).map(PhysicsState::getVelocity);
    }

    public void setVelocity(NPC<?> npc, Vector3d velocity) {
        if (velocity == null) {
            throw new IllegalArgumentException("Velocity cannot be null");
        }

        if (!isValidVector(velocity)) {
            velocity = Vector3d.ZERO;
        }

        PhysicsState state = entities.get(npc.getId());
        if (state != null) {
            state.setVelocity(velocity);

            // If velocity has positive Y (upward force like knockback/jump),
            // mark as not on ground so physics applies correctly
            if (velocity.getY() > 0.01) {
                state.setOnGround(false);
            }
        }
    }

    public void setTargetVelocity(NPC<?> npc, Vector3d targetVelocity) {
        if (targetVelocity != null && !isValidVector(targetVelocity)) {
            return;
        }

        PhysicsState state = entities.get(npc.getId());
        if (state != null) {
            state.setTargetVelocity(targetVelocity);
        }
    }

    /**
     * Checks if a vector contains valid (non-NaN, non-infinite) values.
     *
     * @param vector the vector to check
     * @return true if valid, false otherwise
     */
    private boolean isValidVector(Vector3d vector) {
        return !Double.isNaN(vector.getX()) && !Double.isInfinite(vector.getX()) &&
               !Double.isNaN(vector.getY()) && !Double.isInfinite(vector.getY()) &&
               !Double.isNaN(vector.getZ()) && !Double.isInfinite(vector.getZ());
    }

    public Optional<Vector3d> getTargetVelocity(NPC<?> npc) {
        PhysicsState state = entities.get(npc.getId());
        return Optional.ofNullable(state).map(PhysicsState::getTargetVelocity);
    }

    public Optional<Boolean> isOnGround(NPC<?> npc) {
        PhysicsState state = entities.get(npc.getId());
        return Optional.ofNullable(state).map(PhysicsState::isOnGround);
    }

    private @NotNull Vector3d getVector3d(PhysicsState state, float deltaTime, NPC<?> npc) {
        Vector3d v = state.getVelocity();
        Vector3d targetVel = state.getTargetVelocity();

        // CRITICAL: If velocity has significant upward component, treat as airborne
        // This handles knockback/jumps even if onGround flag hasn't updated yet
        boolean effectivelyOnGround = state.isOnGround() && v.getY() <= 0.1;

        // Check for liquid physics
        if (state.isInWater()) {
            v = applyWaterPhysics(state, v, deltaTime, npc);
        } else if (state.isInLava()) {
            v = applyLavaPhysics(state, v, deltaTime);
        } else if (!effectivelyOnGround) {
            // Normal air physics
            double force = GRAVITY * state.getGravityMultiplier() * deltaTime;
            v = v.add(0, force, 0);

            if (v.getY() < TERMINAL_VELOCITY) {
                v = new Vector3d(v.getX(), TERMINAL_VELOCITY, v.getZ());
            }

            v = new Vector3d(v.getX() * AIR_RESISTANCE, v.getY() * AIR_RESISTANCE, v.getZ() * AIR_RESISTANCE);
        } else {
            // Ground physics
            if (targetVel != null) {
                v = new Vector3d(targetVel.getX(), v.getY(), targetVel.getZ());
            } else {
                v = new Vector3d(v.getX() * FRICTION, v.getY(), v.getZ() * FRICTION);

                if (Math.abs(v.getX()) < VELOCITY_THRESHOLD && Math.abs(v.getZ()) < VELOCITY_THRESHOLD) {
                    v = new Vector3d(0, v.getY(), 0);
                }
            }
        }
        return v;
    }

    /**
     * Applies water physics - entities sink slowly with heavy drag.
     * Much slower than air falling, with bubble column support.
     */
    private Vector3d applyWaterPhysics(PhysicsState state, Vector3d v, float deltaTime, NPC<?> npc) {
        Position pos = npc.getPosition();
        WorldProvider worldProvider = collisionManager.getWorldProvider();

        // Check for bubble column at entity position
        BlockPosition blockPos = BlockPosition.of(
            pos.worldId(),
            (int) Math.floor(pos.x()),
            (int) Math.floor(pos.y()),
            (int) Math.floor(pos.z())
        );

        WorldProvider.BubbleColumnType bubbleType = worldProvider.getBubbleColumnType(blockPos);

        double vy = v.getY();

        if (bubbleType == WorldProvider.BubbleColumnType.UPWARD) {
            // Soul sand bubble column - push up strongly
            vy = Math.min(vy + BUBBLE_COLUMN_UP_SPEED * deltaTime * 20, BUBBLE_COLUMN_UP_SPEED);
        } else if (bubbleType == WorldProvider.BubbleColumnType.DOWNWARD) {
            // Magma bubble column - pull down faster
            vy = Math.max(vy - BUBBLE_COLUMN_DOWN_SPEED * deltaTime * 20, -BUBBLE_COLUMN_DOWN_SPEED);
        } else {
            // Normal water - slow sinking with drag
            // Apply very gentle gravity (much less than air)
            vy = vy + WATER_GRAVITY;

            // Apply vertical drag to slow down falling
            vy = vy * WATER_VERTICAL_DRAG;

            // Clamp to water terminal velocity (slow sink, not instant)
            if (vy < WATER_TERMINAL_VELOCITY) {
                vy = WATER_TERMINAL_VELOCITY;
            }
        }

        // Apply horizontal water drag
        double vx = v.getX() * WATER_DRAG;
        double vz = v.getZ() * WATER_DRAG;

        return new Vector3d(vx, vy, vz);
    }

    /**
     * Applies lava physics - very heavy drag and slow movement.
     */
    private Vector3d applyLavaPhysics(PhysicsState state, Vector3d v, float deltaTime) {
        // Lava has very heavy drag and slow gravity
        double force = LAVA_GRAVITY * state.getGravityMultiplier() * deltaTime;
        double vy = v.getY() + force;

        // Lava terminal velocity
        if (vy < TERMINAL_VELOCITY * 0.25) {
            vy = TERMINAL_VELOCITY * 0.25;
        }

        // Apply heavy lava drag
        double vx = v.getX() * LAVA_DRAG;
        double vz = v.getZ() * LAVA_DRAG;
        vy = vy * LAVA_DRAG;

        return new Vector3d(vx, vy, vz);
    }

    /**
     * Processes environmental effects like fire, water, lava at the entity's position.
     * Handles ignition, extinguishing, fire damage, and drowning.
     */
    private void processEnvironmentalPhysics(PhysicsState state, Position pos) {
        WorldProvider worldProvider = collisionManager.getWorldProvider();
        NPC<?> npc = state.getNpc();

        // Get block positions to check (feet level and eye level)
        BlockPosition feetPos = BlockPosition.of(
            pos.worldId(),
            (int) Math.floor(pos.x()),
            (int) Math.floor(pos.y()),
            (int) Math.floor(pos.z())
        );

        BlockPosition eyePos = BlockPosition.of(
            pos.worldId(),
            (int) Math.floor(pos.x()),
            (int) Math.floor(pos.y() + 1.5), // Approximate eye height
            (int) Math.floor(pos.z())
        );

        // Check for water/lava at feet or body
        boolean inWater = worldProvider.isBlockWater(feetPos) || worldProvider.isBlockWater(eyePos);
        boolean inLava = worldProvider.isBlockLava(feetPos) || worldProvider.isBlockLava(eyePos);
        boolean inFire = worldProvider.isBlockFire(feetPos);
        boolean isRaining = worldProvider.isRainingAt(feetPos);

        // Update liquid state
        boolean wasInWater = state.isInWater();
        state.setInWater(inWater);
        state.setInLava(inLava);

        // Handle fire ignition and extinguishing
        if (!state.isFireImmune()) {
            if (inLava) {
                // Lava sets fire for longer and deals immediate damage
                state.setFireTicks(Math.max(state.getFireTicks(), LAVA_FIRE_TICKS));
                applyLavaDamage(state, npc);
            } else if (inFire && !inWater) {
                // Fire block ignites entity
                state.setFireTicks(Math.max(state.getFireTicks(), FIRE_BLOCK_TICKS));
            }
        }

        // Water or rain extinguishes fire
        if (inWater || isRaining) {
            if (state.isOnFire()) {
                state.extinguish();
                syncFireVisual(npc, false);
            }
        }

        // Process fire ticks and damage
        if (state.isOnFire() && !state.isFireImmune()) {
            processFireDamage(state, npc);
        }

        // Handle drowning
        if (inWater) {
            // Check if head is submerged (eye level in water)
            boolean headSubmerged = worldProvider.isBlockWater(eyePos);
            if (headSubmerged) {
                processDrowning(state, npc);
            } else {
                // Head above water - recover air slowly
                state.setAirSupply(Math.min(state.getAirSupply() + AIR_RECOVERY_RATE, state.getMaxAirSupply()));
            }
        } else if (wasInWater) {
            // Just left water - recover air
            state.restoreAirSupply();
        }

        // Sync fire visual if fire state changed
        boolean currentlyOnFire = state.isOnFire();
        // Visual sync is handled by the metadata system
    }

    /**
     * Processes fire damage - deals damage every second while on fire.
     */
    private void processFireDamage(PhysicsState state, NPC<?> npc) {
        // Decrement fire ticks
        state.setFireTicks(state.getFireTicks() - 1);

        // Increment damage timer
        state.setFireDamageTimer(state.getFireDamageTimer() + 1);

        // Deal damage every FIRE_DAMAGE_INTERVAL ticks
        if (state.getFireDamageTimer() >= FIRE_DAMAGE_INTERVAL) {
            state.setFireDamageTimer(0);
            applyDamage(npc, FIRE_DAMAGE, "fire");
        }

        // Sync fire visual
        if (state.getFireTicks() <= 0) {
            state.extinguish();
            syncFireVisual(npc, false);
        } else {
            syncFireVisual(npc, true);
        }
    }

    /**
     * Applies immediate lava damage.
     */
    private void applyLavaDamage(PhysicsState state, NPC<?> npc) {
        state.setFireDamageTimer(state.getFireDamageTimer() + 1);

        if (state.getFireDamageTimer() >= FIRE_DAMAGE_INTERVAL / 2) { // Lava damages twice as fast
            state.setFireDamageTimer(0);
            applyDamage(npc, LAVA_DAMAGE, "lava");
        }
    }

    /**
     * Processes drowning when head is submerged.
     */
    private void processDrowning(PhysicsState state, NPC<?> npc) {
        // Decrease air supply
        state.setAirSupply(state.getAirSupply() - 1);

        if (state.getAirSupply() <= 0) {
            // Out of air - start drowning damage
            state.setDrownDamageTimer(state.getDrownDamageTimer() + 1);

            if (state.getDrownDamageTimer() >= DROWN_DAMAGE_INTERVAL) {
                state.setDrownDamageTimer(0);
                applyDamage(npc, DROWN_DAMAGE, "drowning");
            }
        }
    }

    /**
     * Applies damage to an NPC if it has a combatant component with health.
     */
    private void applyDamage(NPC<?> npc, float amount, String source) {
        npc.getComponent(prisons.solar.npclib.api.component.CombatantComponent.class).ifPresent(combatant -> {
            var health = combatant.healthComponent();
            if (health == null) {
                return;
            }

            prisons.solar.npclib.api.health.HealthComponent.DamageType damageType;
            switch (source) {
                case "fire" -> damageType = prisons.solar.npclib.api.health.HealthComponent.DamageType.FIRE;
                case "lava" -> damageType = prisons.solar.npclib.api.health.HealthComponent.DamageType.LAVA;
                case "drowning" -> damageType = prisons.solar.npclib.api.health.HealthComponent.DamageType.DROWNING;
                default -> damageType = prisons.solar.npclib.api.health.HealthComponent.DamageType.CUSTOM;
            }

            prisons.solar.npclib.api.health.HealthComponent.DamageSource damageSource =
                prisons.solar.npclib.api.health.HealthComponent.DamageSource.of(
                    damageType, null, null, npc.getPosition()
                );

            health.damage(damageSource, amount);
        });
    }

    /**
     * Syncs the fire visual to all viewers via the NPC's appearance.
     */
    private void syncFireVisual(NPC<?> npc, boolean onFire) {
        // Update the fire visual through the appearance system
        var appearance = npc.appearance();
        if (appearance.isOnFire() != onFire) {
            appearance.setOnFire(onFire);
            appearance.markDirty();
        }
    }

    // Public methods for external fire/water state access

    /**
     * Sets an NPC on fire for the specified number of ticks.
     *
     * @param npc the NPC to ignite
     * @param ticks fire duration in ticks (20 ticks = 1 second)
     */
    public void setOnFire(NPC<?> npc, int ticks) {
        PhysicsState state = entities.get(npc.getId());
        if (state != null && !state.isFireImmune()) {
            state.setFireTicks(ticks);
            syncFireVisual(npc, true);
        }
    }

    /**
     * Extinguishes fire on an NPC.
     *
     * @param npc the NPC to extinguish
     */
    public void extinguish(NPC<?> npc) {
        PhysicsState state = entities.get(npc.getId());
        if (state != null) {
            state.extinguish();
            syncFireVisual(npc, false);
        }
    }

    /**
     * Checks if an NPC is currently on fire.
     *
     * @param npc the NPC to check
     * @return true if on fire
     */
    public boolean isOnFire(NPC<?> npc) {
        PhysicsState state = entities.get(npc.getId());
        return state != null && state.isOnFire();
    }

    /**
     * Checks if an NPC is currently in water.
     *
     * @param npc the NPC to check
     * @return true if in water
     */
    public boolean isInWater(NPC<?> npc) {
        PhysicsState state = entities.get(npc.getId());
        return state != null && state.isInWater();
    }

    /**
     * Checks if an NPC is currently in lava.
     *
     * @param npc the NPC to check
     * @return true if in lava
     */
    public boolean isInLava(NPC<?> npc) {
        PhysicsState state = entities.get(npc.getId());
        return state != null && state.isInLava();
    }

    /**
     * Sets fire immunity for an NPC.
     *
     * @param npc the NPC
     * @param immune true to make immune to fire
     */
    public void setFireImmune(NPC<?> npc, boolean immune) {
        PhysicsState state = entities.get(npc.getId());
        if (state != null) {
            state.setFireImmune(immune);
            if (immune && state.isOnFire()) {
                state.extinguish();
                syncFireVisual(npc, false);
            }
        }
    }

    /**
     * Gets the remaining air supply for an NPC.
     *
     * @param npc the NPC
     * @return air supply in ticks, or -1 if NPC not registered
     */
    public int getAirSupply(NPC<?> npc) {
        PhysicsState state = entities.get(npc.getId());
        return state != null ? state.getAirSupply() : -1;
    }
}
