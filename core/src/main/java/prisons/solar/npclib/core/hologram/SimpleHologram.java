package prisons.solar.npclib.core.hologram;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.hologram.Hologram;
import prisons.solar.npclib.api.npc.Position;
import prisons.solar.npclib.api.viewer.Viewer;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Simple implementation of {@link Hologram}.
 * Uses text display entities (1.19.4+) when available.
 */
public class SimpleHologram implements Hologram {

    private final String id;
    private final List<String> lines = new CopyOnWriteArrayList<>();
    private final Set<Viewer> viewers = Collections.newSetFromMap(new WeakHashMap<>());
    private final List<Integer> entityIds = new ArrayList<>();

    private Position position;
    private double lineSpacing = 0.25;
    private boolean destroyed = false;

    // Callbacks for spawning/despawning - set by platform implementation
    private BiConsumer<Viewer, List<HologramLine>> spawnCallback;
    private BiConsumer<Viewer, List<Integer>> despawnCallback;

    public SimpleHologram(@NotNull String id, @NotNull Position position) {
        this.id = id;
        this.position = position;
    }

    /**
     * Sets the spawn callback for platform-specific spawning.
     *
     * @param callback the callback
     */
    public void setSpawnCallback(BiConsumer<Viewer, List<HologramLine>> callback) {
        this.spawnCallback = callback;
    }

    /**
     * Sets the despawn callback for platform-specific despawning.
     *
     * @param callback the callback
     */
    public void setDespawnCallback(BiConsumer<Viewer, List<Integer>> callback) {
        this.despawnCallback = callback;
    }

    /**
     * Allocates entity IDs for lines.
     *
     * @param idAllocator function to allocate entity IDs
     */
    public void allocateEntityIds(java.util.function.IntSupplier idAllocator) {
        entityIds.clear();
        for (int i = 0; i < lines.size(); i++) {
            entityIds.add(idAllocator.getAsInt());
        }
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull Position position() {
        return position;
    }

    @Override
    public void setPosition(@NotNull Position position) {
        this.position = position;
        update();
    }

    @Override
    public @NotNull List<String> lines() {
        return Collections.unmodifiableList(lines);
    }

    @Override
    public void setLines(@NotNull List<String> lines) {
        this.lines.clear();
        this.lines.addAll(lines);
        update();
    }

    @Override
    public void setLine(int index, @NotNull String line) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, line);
            update();
        }
    }

    @Override
    public void addLine(@NotNull String line) {
        lines.add(line);
        update();
    }

    @Override
    public void insertLine(int index, @NotNull String line) {
        if (index >= 0 && index <= lines.size()) {
            lines.add(index, line);
            update();
        }
    }

    @Override
    public void removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            lines.remove(index);
            update();
        }
    }

    @Override
    public void clearLines() {
        lines.clear();
        update();
    }

    @Override
    public double lineSpacing() {
        return lineSpacing;
    }

    @Override
    public void setLineSpacing(double spacing) {
        this.lineSpacing = spacing;
        update();
    }

    @Override
    public void spawn(@NotNull Viewer viewer) {
        if (destroyed || viewers.contains(viewer)) {
            return;
        }

        viewers.add(viewer);

        if (spawnCallback != null) {
            spawnCallback.accept(viewer, buildLines());
        }
    }

    @Override
    public void despawn(@NotNull Viewer viewer) {
        if (!viewers.remove(viewer)) {
            return;
        }

        if (despawnCallback != null) {
            despawnCallback.accept(viewer, new ArrayList<>(entityIds));
        }
    }

    @Override
    public void despawn() {
        for (Viewer viewer : new ArrayList<>(viewers)) {
            despawn(viewer);
        }
    }

    @Override
    public void destroy() {
        despawn();
        destroyed = true;
        lines.clear();
        entityIds.clear();
    }

    @Override
    public @NotNull Collection<Viewer> viewers() {
        return Collections.unmodifiableSet(new HashSet<>(viewers));
    }

    @Override
    public boolean isVisibleTo(@NotNull Viewer viewer) {
        return viewers.contains(viewer);
    }

    @Override
    public void update() {
        if (destroyed) {
            return;
        }

        // Re-spawn for all viewers to update
        List<Viewer> currentViewers = new ArrayList<>(viewers);
        for (Viewer viewer : currentViewers) {
            if (despawnCallback != null) {
                despawnCallback.accept(viewer, new ArrayList<>(entityIds));
            }
        }

        // Clear entity IDs and allocate new ones if needed
        // (handled by platform implementation)

        for (Viewer viewer : currentViewers) {
            if (spawnCallback != null) {
                spawnCallback.accept(viewer, buildLines());
            }
        }
    }

    private List<HologramLine> buildLines() {
        List<HologramLine> result = new ArrayList<>();
        double currentY = position.y();

        for (int i = 0; i < lines.size(); i++) {
            int entityId = i < entityIds.size() ? entityIds.get(i) : -1;
            Position linePos = Position.of(position.worldId(), position.x(), currentY, position.z());
            result.add(new HologramLine(entityId, lines.get(i), linePos));
            currentY -= lineSpacing;
        }

        return result;
    }

    /**
     * Represents a single line in the hologram.
     *
     * @param entityId the entity ID for this line
     * @param text     the text content
     * @param position the position
     */
    public record HologramLine(int entityId, String text, Position position) {}
}
