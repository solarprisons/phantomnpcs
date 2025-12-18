package prisons.solar.npclib.core.spatial;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.npc.NPC;
import prisons.solar.npclib.api.npc.Position;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spatial hash grid for efficient NPC range queries.
 * Divides the world into cells and tracks which NPCs are in each cell.
 */
public class SpatialIndex {

    private final double cellSize;
    private final Map<Long, Set<NPC<?>>> cells = new ConcurrentHashMap<>();
    private final Map<UUID, Long> npcCells = new ConcurrentHashMap<>();

    /**
     * Creates a spatial index with the specified cell size.
     *
     * @param cellSize the cell size in blocks (recommended: view distance)
     */
    public SpatialIndex(double cellSize) {
        this.cellSize = cellSize;
    }

    /**
     * Inserts or updates an NPC in the index.
     *
     * @param npc the NPC
     */
    public void update(@NotNull NPC<?> npc) {
        Position pos = npc.position();
        long newCell = hash(pos.x(), pos.z(), pos.worldId());

        Long oldCell = npcCells.get(npc.id());
        if (oldCell != null && oldCell == newCell) {
            return; // No change
        }

        // Remove from old cell
        if (oldCell != null) {
            Set<NPC<?>> oldSet = cells.get(oldCell);
            if (oldSet != null) {
                oldSet.remove(npc);
                if (oldSet.isEmpty()) {
                    cells.remove(oldCell);
                }
            }
        }

        // Add to new cell
        cells.computeIfAbsent(newCell, k -> ConcurrentHashMap.newKeySet()).add(npc);
        npcCells.put(npc.id(), newCell);
    }

    /**
     * Removes an NPC from the index.
     *
     * @param npc the NPC
     */
    public void remove(@NotNull NPC<?> npc) {
        Long cell = npcCells.remove(npc.id());
        if (cell != null) {
            Set<NPC<?>> set = cells.get(cell);
            if (set != null) {
                set.remove(npc);
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
    public Collection<NPC<?>> findInRange(@NotNull Position center, double radius) {
        List<NPC<?>> result = new ArrayList<>();
        double radiusSq = radius * radius;

        // Calculate which cells we need to check
        int cellRadius = (int) Math.ceil(radius / cellSize);
        int centerCellX = (int) Math.floor(center.x() / cellSize);
        int centerCellZ = (int) Math.floor(center.z() / cellSize);

        for (int dx = -cellRadius; dx <= cellRadius; dx++) {
            for (int dz = -cellRadius; dz <= cellRadius; dz++) {
                long cell = hash(centerCellX + dx, centerCellZ + dz, center.worldId());
                Set<NPC<?>> npcs = cells.get(cell);

                if (npcs != null) {
                    for (NPC<?> npc : npcs) {
                        Position npcPos = npc.position();
                        if (!npcPos.worldId().equals(center.worldId())) {
                            continue;
                        }

                        if (npcPos.distanceSquared(center) <= radiusSq) {
                            result.add(npc);
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
    public Collection<NPC<?>> findInWorld(@NotNull String worldId) {
        List<NPC<?>> result = new ArrayList<>();

        for (Set<NPC<?>> npcs : cells.values()) {
            for (NPC<?> npc : npcs) {
                if (npc.position().worldId().equals(worldId)) {
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

    private long hash(double x, double z, String worldId) {
        int cellX = (int) Math.floor(x / cellSize);
        int cellZ = (int) Math.floor(z / cellSize);
        return hash(cellX, cellZ, worldId);
    }

    private long hash(int cellX, int cellZ, String worldId) {
        // Combine cell coordinates with world hash
        long worldHash = worldId.hashCode() & 0xFFFFL;
        long cellHash = ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
        return cellHash ^ (worldHash << 48);
    }
}
