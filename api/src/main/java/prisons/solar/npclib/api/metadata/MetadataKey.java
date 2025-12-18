package prisons.solar.npclib.api.metadata;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A type-safe key for storing metadata on NPCs.
 *
 * @param <T> the type of value this key holds
 */
public final class MetadataKey<T> {
    private final String key;
    private final Class<T> type;

    private MetadataKey(String key, Class<T> type) {
        this.key = Objects.requireNonNull(key, "key");
        this.type = Objects.requireNonNull(type, "type");
    }

    /**
     * Creates a new metadata key.
     *
     * @param key  the string key identifier
     * @param type the class type of the value
     * @param <T>  the value type
     * @return a new metadata key
     */
    public static <T> MetadataKey<T> of(@NotNull String key, @NotNull Class<T> type) {
        return new MetadataKey<>(key, type);
    }

    /**
     * Gets the string key identifier.
     *
     * @return the key
     */
    public @NotNull String key() {
        return key;
    }

    /**
     * Gets the value type class.
     *
     * @return the type class
     */
    public @NotNull Class<T> type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetadataKey<?> that)) return false;
        return key.equals(that.key) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, type);
    }

    @Override
    public String toString() {
        return "MetadataKey{" + key + ", " + type.getSimpleName() + "}";
    }
}
