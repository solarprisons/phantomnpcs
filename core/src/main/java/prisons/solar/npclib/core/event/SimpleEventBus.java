package prisons.solar.npclib.core.event;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.EventBus;
import prisons.solar.npclib.api.event.npc.NPCEvent;

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
        List<HandlerEntry<?>> list = handlers.get(event.getClass());
        if (list != null) {
            for (HandlerEntry<?> entry : list) {
                try {
                    ((Consumer<T>) entry.handler).accept(event);
                } catch (Exception e) {
                    // Swallow handler exceptions to prevent event propagation failures
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