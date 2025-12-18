package prisons.solar.npclib.core.event;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.EventBus;
import prisons.solar.npclib.api.event.NPCEvent;

import java.util.*;
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
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(entry);
        // Sort by priority
        handlers.get(eventType).sort(Comparator.comparingInt(e -> e.priority.ordinal()));

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
        List<HandlerEntry<?>> list = handlers.get(event.getClass());
        if (list != null) {
            for (HandlerEntry<?> entry : list) {
                try {
                    ((Consumer<T>) entry.handler).accept(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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