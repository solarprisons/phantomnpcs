package prisons.solar.npclib.api.event;

import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.api.event.npc.NPCEvent;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Event bus for NPC-related events.
 */
public interface EventBus {

    /**
     * Subscribes to an event type.
     *
     * @param eventType the event class
     * @param handler   the event handler
     * @param <T>       the event type
     * @return subscription that can be used to unsubscribe
     */
    <T extends NPCEvent> @NotNull Subscription subscribe(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler
    );

    /**
     * Subscribes to an event type with a priority.
     *
     * @param eventType the event class
     * @param handler   the event handler
     * @param priority  the handler priority
     * @param <T>       the event type
     * @return subscription
     */
    <T extends NPCEvent> @NotNull Subscription subscribe(
            @NotNull Class<T> eventType,
            @NotNull Consumer<T> handler,
            @NotNull Priority priority
    );

    /**
     * Posts an event synchronously.
     *
     * @param event the event to post
     * @param <T>   the event type
     * @return the event after all handlers have processed it
     */
    <T extends NPCEvent> @NotNull T post(@NotNull T event);

    /**
     * Posts an event asynchronously.
     *
     * @param event the event to post
     * @param <T>   the event type
     * @return future that completes with the processed event
     */
    <T extends NPCEvent> @NotNull CompletableFuture<T> postAsync(@NotNull T event);

    /**
     * Represents an event subscription.
     */
    interface Subscription {
        /**
         * Unsubscribes this handler.
         */
        void unsubscribe();

        /**
         * Returns whether this subscription is still active.
         *
         * @return true if active
         */
        boolean isActive();
    }

    /**
     * Event handler priorities.
     */
    enum Priority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST,
        /**
         * Monitor priority - should not modify the event.
         */
        MONITOR
    }
}
