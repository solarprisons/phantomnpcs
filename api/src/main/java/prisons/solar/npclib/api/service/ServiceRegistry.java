package prisons.solar.npclib.api.service;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Registry for services and extensions.
 * Services are registered by type and can have priorities.
 */
public interface ServiceRegistry {

    /**
     * Registers a service implementation.
     *
     * @param type           the service interface type
     * @param implementation the implementation
     * @param <T>            the service type
     */
    <T> void register(@NotNull Class<T> type, @NotNull T implementation);

    /**
     * Registers a service implementation with a priority.
     * Higher priority implementations are preferred.
     *
     * @param type           the service interface type
     * @param implementation the implementation
     * @param priority       the priority (higher = preferred)
     * @param <T>            the service type
     */
    <T> void register(@NotNull Class<T> type, @NotNull T implementation, int priority);

    /**
     * Gets the highest-priority implementation of a service.
     *
     * @param type the service interface type
     * @param <T>  the service type
     * @return the implementation if registered
     */
    <T> @NotNull Optional<T> get(@NotNull Class<T> type);

    /**
     * Gets all implementations of a service, ordered by priority (highest first).
     *
     * @param type the service interface type
     * @param <T>  the service type
     * @return list of implementations
     */
    <T> @NotNull List<T> getAll(@NotNull Class<T> type);

    /**
     * Checks if a service type is registered.
     *
     * @param type the service interface type
     * @return true if registered
     */
    boolean has(@NotNull Class<?> type);

    /**
     * Unregisters a service implementation.
     *
     * @param type           the service interface type
     * @param implementation the implementation to remove
     * @param <T>            the service type
     */
    <T> void unregister(@NotNull Class<T> type, @NotNull T implementation);

    /**
     * Unregisters all implementations of a service type.
     *
     * @param type the service interface type
     */
    void unregisterAll(@NotNull Class<?> type);
}
