package prisons.solar.npclib.core.npc;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.metadata.MetadataKey;
import prisons.solar.npclib.api.metadata.NPCMetadata;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of {@link NPCMetadata}.
 */
public class SimpleNPCMetadata implements NPCMetadata {

    private final Map<MetadataKey<?>, Object> data = new ConcurrentHashMap<>();
    private Runnable dirtyCallback;

    /**
     * Sets the callback invoked when metadata changes.
     *
     * @param callback the callback
     */
    public void setDirtyCallback(Runnable callback) {
        this.dirtyCallback = callback;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @NotNull Optional<T> get(@NotNull MetadataKey<T> key) {
        return Optional.ofNullable((T) data.get(key));
    }

    @Override
    public <T> void set(@NotNull MetadataKey<T> key, T value) {
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
        notifyDirty();
    }

    @Override
    public void remove(@NotNull MetadataKey<?> key) {
        data.remove(key);
        notifyDirty();
    }

    @Override
    public boolean has(@NotNull MetadataKey<?> key) {
        return data.containsKey(key);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NotNull Set<MetadataKey<?>> keys() {
        return Set.copyOf(data.keySet());
    }

    @Override
    public void clear() {
        data.clear();
        notifyDirty();
    }

    private void notifyDirty() {
        if (dirtyCallback != null) {
            try {
                dirtyCallback.run();
            } catch (Exception e) {
                // Swallow - metadata operations should not fail due to callback errors
            }
        }
    }
}
