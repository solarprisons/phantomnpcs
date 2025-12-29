package prisons.solar.npclib.core.physics;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.math.MutableVector3d;
import prisons.solar.npclib.api.math.Vector3d;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.world.BlockPosition;
import prisons.solar.npclib.api.world.WorldProvider;
import prisons.solar.npclib.core.pool.AABBPool;
import prisons.solar.npclib.core.pool.ObjectPoolManager;
import prisons.solar.npclib.core.pool.Vector3dPool;
import prisons.solar.npclib.core.spatial.SpatialGrid;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CollisionManager {

    private final Map<String, SpatialGrid> gridByWorld = new ConcurrentHashMap<>();

    private final Map<UUID, CollisionEntity> entities = new ConcurrentHashMap<>();

    private final LoadingCache<@NotNull BlockPosition, BlockCollisionData> blockCache;

    private final WorldProvider worldProvider;

    /**
     * Creates a CollisionManager with default cache size of 100,000 blocks.
     *
     * @param worldProvider the world provider for querying block data
     */
    public CollisionManager(WorldProvider worldProvider) {
        this(worldProvider, 100_000);
    }

    /**
     * Creates a CollisionManager with configurable cache size.
     *
     * @param worldProvider the world provider for querying block data
     * @param cacheSize maximum number of blocks to cache (recommended: 100,000)
     */
    public CollisionManager(WorldProvider worldProvider, int cacheSize) {
        this.worldProvider = worldProvider;
        this.blockCache = Caffeine.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build(this::loadBlockData);
    }

    /**
     * Gets the world provider used by this collision manager.
     *
     * @return the world provider
     */
    public WorldProvider getWorldProvider() {
        return worldProvider;
    }

    public void registerEntity(NPC<?> npc){
        AABB aabb = AABB.fromEntityType(npc.entityType(), npc.getPosition());
        CollisionEntity entity = new CollisionEntity(npc.getId(),npc.getPosition(), aabb, npc.getPosition().worldId());

        entities.put(npc.getId(), entity);
        getGrid(npc.getPosition().worldId()).update(entity);
    }

    public void unregisterEntity(UUID id) {
        CollisionEntity entity = entities.remove(id);
        if (entity != null) getGrid(entity.getWorldId()).remove(entity);
    }

    public void updatePosition(NPC<?> npc, Position newPos) {
        CollisionEntity entity = entities.get(npc.getId());
        if (entity != null) {
            SpatialGrid grid = getGrid(entity.getWorldId());

            // Remove from old position
            grid.remove(entity);

            // Update entity position and AABB
            entity.setPosition(newPos);
            AABB newAABB = AABB.fromEntityType(npc.entityType(), newPos);
            entity.setAabb(newAABB);

            // Re-insert at new position
            grid.update(entity);
        }
    }

    public Vector3d resolveCollision(NPC<?> npc, Vector3d movement) {
        CollisionEntity entity = entities.get(npc.getId());
        if (entity == null) return movement;

        // Early exit for zero movement
        if (movement.lengthSquared() < 0.0001) {
            return movement;
        }

        // Use pooled objects for zero-allocation collision detection
        AABBPool aabbPool = ObjectPoolManager.getInstance().getAABBPool();
        Vector3dPool vectorPool = ObjectPoolManager.getInstance().getVector3dPool();

        AABB currentAABB = entity.getAabb();
        MutableAABB targetAABB = aabbPool.borrow(currentAABB).offset(movement);
        MutableVector3d blockAdjusted = vectorPool.borrow(movement);

        resolveBlockCollisions(npc, currentAABB, targetAABB, blockAdjusted);
        resolveEntityCollisions(npc, currentAABB, targetAABB, blockAdjusted);

        // Return immutable copy for public API
        return blockAdjusted.toImmutable();
    }

    /**
     * Resolves block collisions, modifying the movement vector in-place.
     * Zero-allocation version using mutable objects.
     *
     * @param npc the NPC
     * @param current the current AABB
     * @param target the target AABB (mutable)
     * @param movement the movement vector (modified in-place)
     */
    private void resolveBlockCollisions(NPC<?> npc, AABB current, MutableAABB target, MutableVector3d movement){
        String worldId = npc.getPosition().worldId();

        Collection<BlockPosition> blocks = worldProvider.getBlocksInAABB(
                worldId,
                target.minX(), target.minY(), target.minZ(),
                target.maxX(), target.maxY(), target.maxZ()
        );

        // Early exit if no blocks to check
        if (blocks.isEmpty()) {
            return;
        }

        for (BlockPosition bPos : blocks) {
            BlockCollisionData bData = blockCache.get(bPos);

            if (bData != null && bData.solid()) {
                slideAlongSurface(movement, bData.aabb(), current);

                // Early exit if movement zeroed out
                if (movement.lengthSquared() < 0.0001) {
                    movement.zero();
                    return;
                }
            }
        }
    }

    /**
     * Resolves entity-to-entity collisions, modifying the movement vector in-place.
     * Zero-allocation version using mutable objects.
     *
     * @param npc the NPC
     * @param current the current AABB
     * @param target the target AABB (mutable)
     * @param movement the movement vector (modified in-place)
     */
    private void resolveEntityCollisions(NPC<?> npc, AABB current, MutableAABB target, MutableVector3d movement){
        String worldId = npc.getPosition().worldId();
        SpatialGrid grid = getGrid(worldId);

        // Calculate center and search radius without allocating Position
        double centerX = (current.minX() + current.maxX()) / 2;
        double centerY = (current.minY() + current.maxY()) / 2;
        double centerZ = (current.minZ() + current.maxZ()) / 2;
        double radius = Math.max(
                Math.max(current.maxX() - current.minX(), current.maxY() - current.minY()),
                current.maxZ() - current.minZ()
        );

        // Query nearby entities using zero-allocation version
        Collection<CollisionEntity> nearby = grid.findInRange(worldId, centerX, centerY, centerZ, radius);

        for (CollisionEntity other : nearby) {
            if (other.getId().equals(npc.getId())) continue;

            if (target.intersects(other.getAabb())) {
                pushApart(current, other.getAabb(), movement);
            }
        }
    }

    /**
     * Slides movement along obstacle surface, modifying movement vector in-place.
     * Zero-allocation version using pooled mutable AABB for tests.
     *
     * @param movement the movement vector (modified in-place)
     * @param obstacle the obstacle AABB
     * @param current the current entity AABB
     */
    private void slideAlongSurface(MutableVector3d movement, AABB obstacle, AABB current) {
        AABBPool pool = ObjectPoolManager.getInstance().getAABBPool();

        // Test if we're going to collide
        MutableAABB testTarget = pool.borrow(current).offset(movement);
        if (!testTarget.intersects(obstacle)) {
            return; // No collision, keep full movement
        }

        // We're going to collide - test each axis independently
        double originalX = movement.getX();
        double originalY = movement.getY();
        double originalZ = movement.getZ();

        // Test X axis collision
        MutableAABB testX = pool.borrow(current).offset(originalX, 0, 0);
        if (testX.intersects(obstacle)) {
            movement.setX(0);
        }

        // Test Y axis collision
        MutableAABB testY = pool.borrow(current).offset(0, originalY, 0);
        if (testY.intersects(obstacle)) {
            movement.setY(0);
        }

        // Test Z axis collision
        MutableAABB testZ = pool.borrow(current).offset(0, 0, originalZ);
        if (testZ.intersects(obstacle)) {
            movement.setZ(0);
        }
    }

    /**
     * Gets the spatial grid for a world. Used for NPC position queries during pathfinding.
     *
     * @param worldId the world ID
     * @return the spatial grid for that world
     */
    public SpatialGrid getGrid(String worldId) {
        return gridByWorld.computeIfAbsent(worldId, k -> new SpatialGrid(16));
    }

    /**
     * Calculates penetration vector between two AABBs and stores it in result.
     * Zero-allocation version that reuses the result vector.
     *
     * @param entity the entity AABB
     * @param obstacle the obstacle AABB
     * @param result the vector to store the penetration (modified in-place)
     */
    private void calculatePenetration(AABB entity, AABB obstacle, MutableVector3d result) {
        // Calculate overlap on each axis
        double overlapX = 0;
        double overlapY = 0;
        double overlapZ = 0;

        // Calculate X overlap
        if (entity.maxX() > obstacle.minX() && entity.minX() < obstacle.maxX()) {
            double leftOverlap = entity.maxX() - obstacle.minX();
            double rightOverlap = obstacle.maxX() - entity.minX();
            overlapX = leftOverlap < rightOverlap ? -leftOverlap : rightOverlap;
        }

        // Calculate Y overlap
        if (entity.maxY() > obstacle.minY() && entity.minY() < obstacle.maxY()) {
            double bottomOverlap = entity.maxY() - obstacle.minY();
            double topOverlap = obstacle.maxY() - entity.minY();
            overlapY = bottomOverlap < topOverlap ? -bottomOverlap : topOverlap;
        }

        // Calculate Z overlap
        if (entity.maxZ() > obstacle.minZ() && entity.minZ() < obstacle.maxZ()) {
            double frontOverlap = entity.maxZ() - obstacle.minZ();
            double backOverlap = obstacle.maxZ() - entity.minZ();
            overlapZ = frontOverlap < backOverlap ? -frontOverlap : backOverlap;
        }

        result.set(overlapX, overlapY, overlapZ);
    }

    /**
     * Pushes two entities apart by adjusting movement vector in-place.
     * Uses the minimum separation axis to prevent overlap.
     * Zero-allocation version using pooled vector for penetration calculation.
     *
     * @param current the current entity AABB
     * @param other the other entity AABB
     * @param movement the movement vector (modified in-place)
     */
    private void pushApart(AABB current, AABB other, MutableVector3d movement) {
        Vector3dPool pool = ObjectPoolManager.getInstance().getVector3dPool();

        // Calculate penetration using pooled vector
        MutableVector3d penetration = pool.borrowZero();
        calculatePenetration(current, other, penetration);

        // Find the axis with minimum penetration (easiest to separate)
        double absX = Math.abs(penetration.getX());
        double absY = Math.abs(penetration.getY());
        double absZ = Math.abs(penetration.getZ());

        // Push apart on the axis with least overlap
        if (absX < absY && absX < absZ && absX > 0.001) {
            // Separate on X axis
            movement.setX(movement.getX() + penetration.getX());
        } else if (absY < absZ && absY > 0.001) {
            // Separate on Y axis
            movement.setY(movement.getY() + penetration.getY());
        } else if (absZ > 0.001) {
            // Separate on Z axis
            movement.setZ(movement.getZ() + penetration.getZ());
        }
    }

    private BlockCollisionData loadBlockData(BlockPosition pos) {
        WorldProvider.BlockBoundingBox bounds = worldProvider.getBlockBounds(pos);

        if (bounds == null) {
            return null;
        }

        AABB aabb = new AABB(
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ()
        );

        return new BlockCollisionData(pos, bounds.solid(), aabb);
    }

    /**
     * Invalidates a single block in the cache.
     * Call this when a block is placed or broken to force reload.
     *
     * @param position the block position to invalidate
     */
    public void invalidateBlockCache(@NotNull BlockPosition position) {
        blockCache.invalidate(position);
    }

    /**
     * Invalidates a region of blocks in the cache.
     * Call this when multiple blocks change (e.g., explosions, structure generation).
     *
     * @param worldId the world UUID
     * @param minX minimum X coordinate
     * @param minY minimum Y coordinate
     * @param minZ minimum Z coordinate
     * @param maxX maximum X coordinate
     * @param maxY maximum Y coordinate
     * @param maxZ maximum Z coordinate
     */
    public void invalidateBlockRegion(@NotNull String worldId,
                                       int minX, int minY, int minZ,
                                       int maxX, int maxY, int maxZ) {
        // Validate bounds to prevent infinite loops
        if (maxX < minX || maxY < minY || maxZ < minZ) {
            throw new IllegalArgumentException("Invalid region bounds: max must be >= min");
        }

        // Prevent excessive invalidation (more than 100,000 blocks)
        // Large invalidations are allowed but may impact performance
        long totalBlocks = (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blockCache.invalidate(BlockPosition.of(worldId, x, y, z));
                }
            }
        }
    }

    /**
     * Cleans up all entities and grid data for a specific world.
     * Call this when a world is unloaded to prevent memory leaks.
     *
     * @param worldId the world UUID
     * @return number of entities removed
     */
    public int cleanupWorld(@NotNull String worldId) {
        // Remove all entities in this world from the entities map
        int removed = 0;
        Iterator<Map.Entry<UUID, CollisionEntity>> iterator = entities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, CollisionEntity> entry = iterator.next();
            if (entry.getValue().getWorldId().equals(worldId)) {
                iterator.remove();
                removed++;
            }
        }

        // Remove spatial grid for this world (also cleans entities from grid)
        SpatialGrid grid = gridByWorld.remove(worldId);
        if (grid != null) {
            grid.clear();  // Ensure all references are cleared
        }

        return removed;
    }

    /**
     * Invalidates all cached blocks for a specific world.
     * Useful when a world is reloaded or reset.
     *
     * @param worldId the world UUID
     */
    public void invalidateWorldCache(@NotNull String worldId) {
        // Caffeine doesn't have a clean way to invalidate by predicate
        // So we invalidate all and accept the cost
        // In practice, worlds don't reload often enough for this to matter
        blockCache.invalidateAll();
    }

    record BlockCollisionData(BlockPosition position, boolean solid, AABB aabb){}
}
