package prisons.solar.npclib.api.event;

/**
 * Indicates an event that can be cancelled.
 */
public interface Cancellable {

    /**
     * Returns whether this event is cancelled.
     *
     * @return true if cancelled
     */
    boolean isCancelled();

    /**
     * Sets whether this event is cancelled.
     *
     * @param cancelled true to cancel
     */
    void setCancelled(boolean cancelled);
}
