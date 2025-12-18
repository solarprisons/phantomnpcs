package prisons.solar.npclib.platform.scheduler;

/**
 * Represents a scheduled task.
 */
public interface ScheduledTask {

    /**
     * Cancels this task.
     */
    void cancel();

    /**
     * Returns whether this task is cancelled.
     *
     * @return true if cancelled
     */
    boolean isCancelled();

    /**
     * Returns whether this task has completed.
     *
     * @return true if completed
     */
    boolean isDone();
}
