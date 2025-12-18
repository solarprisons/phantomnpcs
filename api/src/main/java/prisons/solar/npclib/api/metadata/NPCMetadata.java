package prisons.solar.npclib.api.metadata;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

/**
 * Holds mutable metadata for an NPC.
 * Changes to metadata automatically trigger synchronization to viewers.
 */
public interface NPCMetadata {

    /**
     * Gets a metadata value by key.
     *
     * @param key the metadata key
     * @param <T> the value type
     * @return the value if present
     */
    <T> @NotNull Optional<T> get(@NotNull MetadataKey<T> key);

    /**
     * Gets a metadata value by key, returning a default if not present.
     *
     * @param key          the metadata key
     * @param defaultValue the default value
     * @param <T>          the value type
     * @return the value or default
     */
    default <T> T getOrDefault(@NotNull MetadataKey<T> key, T defaultValue) {
        return get(key).orElse(defaultValue);
    }

    /**
     * Sets a metadata value.
     *
     * @param key   the metadata key
     * @param value the value to set
     * @param <T>   the value type
     */
    <T> void set(@NotNull MetadataKey<T> key, T value);

    /**
     * Removes a metadata value.
     *
     * @param key the metadata key
     */
    void remove(@NotNull MetadataKey<?> key);

    /**
     * Checks if a metadata key is present.
     *
     * @param key the metadata key
     * @return true if present
     */
    boolean has(@NotNull MetadataKey<?> key);

    /**
     * Gets all metadata keys currently set.
     *
     * @return set of keys
     */
    @NotNull Set<MetadataKey<?>> keys();

    /**
     * Clears all metadata.
     */
    void clear();
}
