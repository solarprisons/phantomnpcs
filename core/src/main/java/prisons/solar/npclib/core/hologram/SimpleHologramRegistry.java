package prisons.solar.npclib.core.hologram;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.hologram.Hologram;
import prisons.solar.npclib.api.hologram.HologramRegistry;
import prisons.solar.npclib.api.npc.Position;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Simple implementation of {@link HologramRegistry}.
 */
public class SimpleHologramRegistry implements HologramRegistry {

    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();
    private final BiFunction<String, Position, Hologram> hologramFactory;

    /**
     * Creates a registry with a default hologram factory.
     */
    public SimpleHologramRegistry() {
        this(SimpleHologram::new);
    }

    /**
     * Creates a registry with a custom hologram factory.
     *
     * @param hologramFactory function to create holograms
     */
    public SimpleHologramRegistry(@NotNull BiFunction<String, Position, Hologram> hologramFactory) {
        this.hologramFactory = hologramFactory;
    }

    @Override
    public @NotNull Hologram create(@NotNull String id, @NotNull Position position) {
        if (holograms.containsKey(id)) {
            throw new IllegalArgumentException("Hologram with ID '" + id + "' already exists");
        }

        Hologram hologram = hologramFactory.apply(id, position);
        holograms.put(id, hologram);
        return hologram;
    }

    @Override
    public @NotNull Optional<Hologram> byId(@NotNull String id) {
        return Optional.ofNullable(holograms.get(id));
    }

    @Override
    public @NotNull Collection<Hologram> all() {
        return Collections.unmodifiableCollection(holograms.values());
    }

    @Override
    public @NotNull Collection<Hologram> inWorld(@NotNull String worldId) {
        return holograms.values().stream()
                .filter(h -> h.position().worldId().equals(worldId))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void remove(@NotNull Hologram hologram) {
        Hologram removed = holograms.remove(hologram.id());
        if (removed != null) {
            removed.destroy();
        }
    }

    @Override
    public void clear() {
        for (Hologram hologram : holograms.values()) {
            hologram.destroy();
        }
        holograms.clear();
    }

    @Override
    public int count() {
        return holograms.size();
    }
}
