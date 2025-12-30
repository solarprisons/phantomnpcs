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

    private static @NotNull Vector3d getVector3d(PhysicsState state, float deltaTime, NPC<?> npc) {
        Vector3d v = state.getVelocity();
        Vector3d targetVel = state.getTargetVelocity();

        // CRITICAL: If velocity has significant upward component, treat as airborne
        // This handles knockback/jumps even if onGround flag hasn't updated yet
        boolean effectivelyOnGround = state.isOnGround() && v.getY() <= 0.1;

        if (!effectivelyOnGround) {
            double force = GRAVITY * state.getGravityMultiplier() * deltaTime;
            v = v.add(0, force, 0);

            if (v.getY() < TERMINAL_VELOCITY) {
                v = new Vector3d(v.getX(), TERMINAL_VELOCITY, v.getZ());
            }

            v = new Vector3d(v.getX() * AIR_RESISTANCE, v.getY() * AIR_RESISTANCE, v.getZ() * AIR_RESISTANCE);
        } else {
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
}
