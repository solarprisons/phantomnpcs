package prisons.solar.npclib.platform.scheduler;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Platform-agnostic task scheduler.
 */
public interface Scheduler {

    /**
     * Runs a task on the main thread.
     *
     * @param task the task to run
     * @return task handle
     */
    @NotNull ScheduledTask runSync(@NotNull Runnable task);

    /**
     * Runs a task asynchronously.
     *
     * @param task the task to run
     * @return task handle
     */
    @NotNull ScheduledTask runAsync(@NotNull Runnable task);

    /**
     * Runs a task on the main thread after a delay.
     *
     * @param task  the task to run
     * @param delay the delay
     * @param unit  the time unit
     * @return task handle
     */
    @NotNull ScheduledTask runSyncLater(@NotNull Runnable task, long delay, @NotNull TimeUnit unit);

    /**
     * Runs a task asynchronously after a delay.
     *
     * @param task  the task to run
     * @param delay the delay
     * @param unit  the time unit
     * @return task handle
     */
    @NotNull ScheduledTask runAsyncLater(@NotNull Runnable task, long delay, @NotNull TimeUnit unit);

    /**
     * Runs a task repeatedly on the main thread.
     *
     * @param task   the task to run
     * @param delay  initial delay
     * @param period period between executions
     * @param unit   the time unit
     * @return task handle
     */
    @NotNull ScheduledTask runSyncRepeating(@NotNull Runnable task, long delay, long period, @NotNull TimeUnit unit);

    /**
     * Runs a task repeatedly asynchronously.
     *
     * @param task   the task to run
     * @param delay  initial delay
     * @param period period between executions
     * @param unit   the time unit
     * @return task handle
     */
    @NotNull ScheduledTask runAsyncRepeating(@NotNull Runnable task, long delay, long period, @NotNull TimeUnit unit);

    /**
     * Checks if the current thread is the main server thread.
     *
     * @return true if on main thread
     */
    boolean isMainThread();
}
