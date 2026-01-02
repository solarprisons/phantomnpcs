package prisons.solar.npclib.core.event;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.EventBus;
import prisons.solar.npclib.api.event.npc.NPCEvent;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple implementation of {@link EventBus}.
 */
public class SimpleEventBus implements EventBus {

    private final Map<Class<?>, List<HandlerEntry<?>>> handlers = new ConcurrentHashMap<>();

    @Override
    public <T extends NPCEvent> @NotNull Subscription subscribe(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler
    ) {
        return subscribe(eventType, handler, Priority.NORMAL);
    }

    @Override
    public <T extends NPCEvent> @NotNull Subscription subscribe(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler,
            @NotNull Priority priority
    ) {
        HandlerEntry<T> entry = new HandlerEntry<>(handler, priority);
        List<HandlerEntry<?>> list = handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());

        // Use insertion sort for O(n) complexity instead of full re-sort O(n log n)
        int insertIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).priority.ordinal() <= priority.ordinal()) {
                insertIndex = i + 1;
            } else {
                break;
            }
        }
        list.add(insertIndex, entry);

        return new Subscription() {
            private boolean active = true;

            @Override
            public void unsubscribe() {
                if (active) {
                    handlers.get(eventType).remove(entry);
                    active = false;
                }
            }

            @Override
            public boolean isActive() {
                return active;
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends NPCEvent> @NotNull T post(@NotNull T event) {
        // Collect all matching handlers (exact class + interfaces + superclasses)
        Set<HandlerEntry<?>> matchingHandlers = new LinkedHashSet<>();

        // Check exact class
        List<HandlerEntry<?>> exactList = handlers.get(event.getClass());
        if (exactList != null) {
            matchingHandlers.addAll(exactList);
        }

        // Check interfaces (e.g., NPCSpawnEvent)
        for (Class<?> iface : event.getClass().getInterfaces()) {
            List<HandlerEntry<?>> ifaceList = handlers.get(iface);
            if (ifaceList != null) {
                matchingHandlers.addAll(ifaceList);
            }
        }

        // Check superclasses
        Class<?> superClass = event.getClass().getSuperclass();
        while (superClass != null && NPCEvent.class.isAssignableFrom(superClass)) {
            List<HandlerEntry<?>> superList = handlers.get(superClass);
            if (superList != null) {
                matchingHandlers.addAll(superList);
            }
            superClass = superClass.getSuperclass();
        }

        // Fire all matching handlers
        for (HandlerEntry<?> entry : matchingHandlers) {
            try {
                ((Consumer<T>) entry.handler).accept(event);
            } catch (Exception e) {
                // Swallow handler exceptions to prevent event propagation failures
            }
        }
        return event;
    }

    @Override
    public <T extends NPCEvent> @NotNull CompletableFuture<T> postAsync(@NotNull T event) {
        return CompletableFuture.supplyAsync(() -> post(event));
    }

    private record HandlerEntry<T>(Consumer<T> handler, Priority priority) {}
}