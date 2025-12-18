package prisons.solar.npclib.core.service;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.service.ServiceRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple implementation of {@link ServiceRegistry}.
 */
public class SimpleServiceRegistry implements ServiceRegistry {

    private final Map<Class<?>, List<PrioritizedService<?>>> services = new ConcurrentHashMap<>();

    @Override
    public <T> void register(@NotNull Class<T> type, @NotNull T implementation) {
        register(type, implementation, 0);
    }

    @Override
    public <T> void register(@NotNull Class<T> type, @NotNull T implementation, int priority) {
        services.computeIfAbsent(type, k -> new ArrayList<>())
                .add(new PrioritizedService<>(implementation, priority));
        // Sort by priority descending
        services.get(type).sort((a, b) -> Integer.compare(b.priority, a.priority));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @NotNull Optional<T> get(@NotNull Class<T> type) {
        List<PrioritizedService<?>> list = services.get(type);
        if (list == null || list.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of((T) list.getFirst().service);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @NotNull List<T> getAll(@NotNull Class<T> type) {
        List<PrioritizedService<?>> list = services.get(type);
        if (list == null) {
            return List.of();
        }
        return list.stream()
                .map(ps -> (T) ps.service)
                .toList();
    }

    @Override
    public boolean has(@NotNull Class<?> type) {
        List<PrioritizedService<?>> list = services.get(type);
        return list != null && !list.isEmpty();
    }

    @Override
    public <T> void unregister(@NotNull Class<T> type, @NotNull T implementation) {
        List<PrioritizedService<?>> list = services.get(type);
        if (list != null) {
            list.removeIf(ps -> ps.service == implementation);
        }
    }

    @Override
    public void unregisterAll(@NotNull Class<?> type) {
        services.remove(type);
    }

    private record PrioritizedService<T>(T service, int priority) {}
}
