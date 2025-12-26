package xyz.overdyn.dyngui.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import xyz.overdyn.dyngui.DynGui;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class TaskSchedulerImpl implements TaskScheduler {

    private final JavaPlugin PLUGIN;
    private final BukkitScheduler SCHEDULER = Bukkit.getScheduler();
    private final Set<BukkitTask> tasks = Collections.synchronizedSet(new HashSet<>());

    public TaskSchedulerImpl(DynGui dynGui) {
        this.PLUGIN = dynGui.getPlugin();

        SCHEDULER.runTaskTimer(PLUGIN, this::cleanup, 100L, 100L);
    }

    private void register(BukkitTask task) {
        if (task != null) tasks.add(task);
    }

    private BukkitTask trackTask(@NotNull Runnable task, long delay, long period, boolean store) {
        if (period > 0) {
            BukkitTask t = SCHEDULER.runTaskTimer(PLUGIN, task, delay, period);
            if (store) register(t);
            return t;
        } else if (delay > 0) {
            BukkitTask t = SCHEDULER.runTaskLater(PLUGIN, task, delay);
            if (store) register(t);
            return t;
        } else {
            // Одноразовая задача без delay
            return SCHEDULER.runTask(PLUGIN, task);
        }
    }

    private BukkitTask trackTaskAsync(@NotNull Runnable task, long delay, long period, boolean store) {
        if (period > 0) {
            BukkitTask t = SCHEDULER.runTaskTimerAsynchronously(PLUGIN, task, delay, period);
            if (store) register(t);
            return t;
        } else if (delay > 0) {
            BukkitTask t = SCHEDULER.runTaskLaterAsynchronously(PLUGIN, task, delay);
            if (store) register(t);
            return t;
        } else {
            return SCHEDULER.runTaskAsynchronously(PLUGIN, task);
        }
    }

    @Override
    public @NotNull BukkitTask runTask(@NotNull Runnable task) {
        return trackTask(task, 0L, 0L, false);
    }

    @Override
    public @NotNull BukkitTask runTaskAsync(@NotNull Runnable task) {
        return trackTaskAsync(task, 0L, 0L, false);
    }

    @Override
    public @NotNull BukkitTask runTask(@NotNull Runnable task, long delay) {
        return trackTask(task, delay, 0L, true);
    }

    @Override
    public @NotNull BukkitTask runTaskAsync(@NotNull Runnable task, long delay) {
        return trackTaskAsync(task, delay, 0L, true);
    }

    @Override
    public @NotNull BukkitTask runTask(@NotNull Runnable task, long delay, long period) {
        return trackTask(task, delay, period, true);
    }

    @Override
    public @NotNull BukkitTask runTaskAsync(@NotNull Runnable task, long delay, long period) {
        return trackTaskAsync(task, delay, period, true);
    }

    @Override
    public void runTask(@NotNull Consumer<BukkitTask> action) {
        BukkitTask t = runTask(() -> {});
        action.accept(t);
    }

    @Override
    public void runTaskAsync(@NotNull Consumer<BukkitTask> action) {
        BukkitTask t = runTaskAsync(() -> {});
        action.accept(t);
    }

    @Override
    public void runTask(@NotNull Consumer<BukkitTask> action, long delay) {
        BukkitTask t = runTask(() -> {}, delay);
        action.accept(t);
    }

    @Override
    public void runTaskAsync(@NotNull Consumer<BukkitTask> action, long delay) {
        BukkitTask t = runTaskAsync(() -> {}, delay);
        action.accept(t);
    }

    @Override
    public void runTask(@NotNull Consumer<BukkitTask> action, long delay, long period) {
        BukkitTask t = runTask(() -> {}, delay, period);
        action.accept(t);
    }

    @Override
    public void runTaskAsync(@NotNull Consumer<BukkitTask> action, long delay, long period) {
        BukkitTask t = runTaskAsync(() -> {}, delay, period);
        action.accept(t);
    }

    @Override
    public @NotNull Set<BukkitTask> getTasks() {
        cleanup();
        return Collections.unmodifiableSet(tasks);
    }

    @Override
    public void cancelAll() {
        synchronized (tasks) {
            for (BukkitTask task : tasks) {
                if (!task.isCancelled()) task.cancel();
            }
            tasks.clear();
        }
    }

    @Override
    public void cancel(@NotNull BukkitTask task) {
        task.cancel();
        tasks.remove(task);
    }

    @Override
    public void cleanup() {
        tasks.removeIf(BukkitTask::isCancelled);
    }
}
