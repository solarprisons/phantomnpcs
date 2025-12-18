package prisons.solar.npclib.paper;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import prisons.solar.npclib.platform.scheduler.ScheduledTask;
import prisons.solar.npclib.platform.scheduler.Scheduler;

import java.util.concurrent.TimeUnit;

/**
 * Paper implementation of {@link Scheduler}.
 */
public class PaperScheduler implements Scheduler {

    private final Plugin plugin;

    public PaperScheduler(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull ScheduledTask runSync(@NotNull Runnable task) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTask(plugin, task);
        return new PaperScheduledTask(bukkitTask);
    }

    @Override
    public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        return new PaperScheduledTask(bukkitTask);
    }

    @Override
    public @NotNull ScheduledTask runSyncLater(@NotNull Runnable task, long delay, @NotNull TimeUnit unit) {
        long ticks = toTicks(delay, unit);
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, ticks);
        return new PaperScheduledTask(bukkitTask);
    }

    @Override
    public @NotNull ScheduledTask runAsyncLater(@NotNull Runnable task, long delay, @NotNull TimeUnit unit) {
        long ticks = toTicks(delay, unit);
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, ticks);
        return new PaperScheduledTask(bukkitTask);
    }

    @Override
    public @NotNull ScheduledTask runSyncRepeating(@NotNull Runnable task, long delay, long period, @NotNull TimeUnit unit) {
        long delayTicks = toTicks(delay, unit);
        long periodTicks = toTicks(period, unit);
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        return new PaperScheduledTask(bukkitTask);
    }

    @Override
    public @NotNull ScheduledTask runAsyncRepeating(@NotNull Runnable task, long delay, long period, @NotNull TimeUnit unit) {
        long delayTicks = toTicks(delay, unit);
        long periodTicks = toTicks(period, unit);
        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        return new PaperScheduledTask(bukkitTask);
    }

    @Override
    public boolean isMainThread() {
        return Bukkit.isPrimaryThread();
    }

    private long toTicks(long duration, TimeUnit unit) {
        return Math.max(1, unit.toMillis(duration) / 50);
    }

    private static class PaperScheduledTask implements ScheduledTask {
        private final BukkitTask task;

        PaperScheduledTask(BukkitTask task) {
            this.task = task;
        }

        @Override
        public void cancel() {
            task.cancel();
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }

        @Override
        public boolean isDone() {
            return task.isCancelled();
        }
    }
}
