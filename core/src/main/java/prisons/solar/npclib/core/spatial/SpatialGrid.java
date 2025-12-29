package prisons.solar.npclib.core.spatial;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.core.physics.CollisionEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spatial hash grid for efficient NPC range queries.
 * Divides the world into cells and tracks which NPCs are in each cell.
 */
public class SpatialGrid {

    private final double cellSize;
    private final Map<Long, Set<CollisionEntity>> cells = new ConcurrentHashMap<>();
    private final Map<UUID, Long> npcCells = new ConcurrentHashMap<>();

    /**
     * Creates a spatial index with the specified cell size.
     *
     * @param cellSize the cell size in blocks (recommended: view distance)
     */
    public SpatialGrid(double cellSize) {
        this.cellSize = cellSize;
    }

    /**
     * Inserts or updates an NPC in the index.
     *
     * @param entity the CollisionEntity
     */
    public void update(@NotNull CollisionEntity entity) {
        Position pos = entity.getPosition();
        long newCell = hash(pos.x(), pos.z(), pos.worldId());

        Long oldCell = npcCells.get(entity.getId());
        if (oldCell != null && oldCell == newCell) {
            return; // No change
        }

        // Remove from old cell
        removeFromCell(entity, oldCell);

        // Add to new cell
        cells.computeIfAbsent(newCell, k -> ConcurrentHashMap.newKeySet()).add(entity);
        npcCells.put(entity.getId(), newCell);
    }

    /**
     * Removes an NPC from the index.
     *
     * @param npc the NPC
     */
    public void remove(@NotNull CollisionEntity npc) {
        Long cell = npcCells.remove(npc.getId());
        removeFromCell(npc, cell);
    }

    private void removeFromCell(@NotNull CollisionEntity entity, Long cell) {
        if (cell != null) {
            Set<CollisionEntity> set = cells.get(cell);
            if (set != null) {
                set.remove(entity);
                if (set.isEmpty()) {
                    cells.remove(cell);
                }
            }
        }
    }

    /**
     * Finds all NPCs within a radius of a position.
     *
     * @param center the center position
     * @param radius the search radius
     * @return collection of NPCs in range
     */
    public Collection<CollisionEntity> findInRange(@NotNull Position center, double radius) {
        return findInRange(center.worldId(), center.x(), center.y(), center.z(), radius);
    }

    /**
     * Finds all NPCs within a radius of a position (zero-allocation version).
     * Avoids creating Position objects in hot paths.
     *
     * @param worldId the world ID
     * @param centerX the center X coordinate
     * @param centerY the center Y coordinate
     * @param centerZ the center Z coordinate
     * @param radius the search radius
     * @return collection of NPCs in range
     */
    public Collection<CollisionEntity> findInRange(@NotNull String worldId, double centerX, double centerY, double centerZ, double radius) {
        List<CollisionEntity> result = new ArrayList<>();
        double radiusSq = radius * radius;

        // Calculate which cells we need to check
        int cellRadius = (int) Math.ceil(radius / cellSize);
        int centerCellX = (int) Math.floor(centerX / cellSize);
        int centerCellZ = (int) Math.floor(centerZ / cellSize);

        for (int dx = -cellRadius; dx <= cellRadius; dx++) {
            for (int dz = -cellRadius; dz <= cellRadius; dz++) {
                long cell = hash(centerCellX + dx, centerCellZ + dz, worldId);
                Set<CollisionEntity> entities = cells.get(cell);

                if (entities != null) {
                    for (CollisionEntity entity: entities) {
                        Position npcPos = entity.getPosition();
                        // World ID already encoded in hash, but double check for safety
                        // due to potential hash collisions
                        if (!npcPos.worldId().equals(worldId)) {
                            continue;
                        }

                        // Manual distance calculation to avoid Position allocation
                        double dx2 = npcPos.x() - centerX;
                        double dy2 = npcPos.y() - centerY;
                        double dz2 = npcPos.z() - centerZ;
                        double distSq = dx2 * dx2 + dy2 * dy2 + dz2 * dz2;

                        if (distSq <= radiusSq) {
                            result.add(entity);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Finds all NPCs in a specific world.
     *
     * @param worldId the world ID
     * @return collection of NPCs in that world
     */
    public Collection<CollisionEntity> findInWorld(@NotNull String worldId) {
        List<CollisionEntity> result = new ArrayList<>();

        for (Set<CollisionEntity> entities : cells.values()) {
            for (CollisionEntity npc : entities) {
                if (npc.getPosition().worldId().equals(worldId)) {
                    result.add(npc);
                }
            }
        }

        return result;
    }

    /**
     * Gets the total number of indexed NPCs.
     *
     * @return NPC count
     */
    public int size() {
        return npcCells.size();
    }

    /**
     * Gets the number of active cells.
     *
     * @return cell count
     */
    public int cellCount() {
        return cells.size();
    }

    /**
     * Clears the index.
     */
    public void clear() {
        cells.clear();
        npcCells.clear();
    }

    /**
     * Removes all entities from a specific world.
     * More efficient than iterating all and checking.
     *
     * @param worldId the world ID to clear
     * @return number of entities removed
     */
    public int clearWorld(@NotNull String worldId) {
        int removed = 0;

        // Iterate through all cells and remove entities from this world
        for (Set<CollisionEntity> entitySet : cells.values()) {
            entitySet.removeIf(entity -> {
                if (entity.getPosition().worldId().equals(worldId)) {
                    npcCells.remove(entity.getId());
                    return true;
                }
                return false;
            });
        }

        // Clean up empty cells
        cells.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        return removed;
    }

    private long hash(double x, double z, String worldId) {
        int cellX = (int) Math.floor(x / cellSize);
        int cellZ = (int) Math.floor(z / cellSize);
        return hash(cellX, cellZ, worldId);
    }

    private long hash(int cellX, int cellZ, String worldId) {
        // Improved hash function to minimize world collisions
        // Use stronger world hash by combining multiple bits
        long worldHash = ((long) worldId.hashCode()) & 0xFFFFFFFFL;

        // Spread cell coordinates across 64 bits more evenly
        // Use prime numbers to reduce collision probability
        long cellHash = (((long) cellX * 73856093L) ^ ((long) cellZ * 19349663L));

        // Combine with world hash using XOR and rotation
        return cellHash ^ (worldHash << 32) ^ (worldHash >>> 32);
    }
}
