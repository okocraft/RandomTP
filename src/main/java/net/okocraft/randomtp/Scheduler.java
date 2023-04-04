package net.okocraft.randomtp;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class Scheduler {

    private static final boolean FOLIA;

    static {
        boolean isFolia;

        try {
            Bukkit.class.getDeclaredMethod("getAsyncScheduler");
            isFolia = true;
        } catch (NoSuchMethodException e) {
            isFolia = false;
        }

        FOLIA = isFolia;
    }

    public static void runOnPlayerScheduler(@NotNull Player player, @NotNull Consumer<Player> task) {
        if (FOLIA) {
            player.getScheduler().run(plugin(), $ -> task.accept(player), null);
        } else {
            Bukkit.getScheduler().runTask(plugin(), () -> task.accept(player));
        }
    }

    public static void runAsync(@NotNull Runnable task) {
        if (FOLIA) {
            Bukkit.getAsyncScheduler().runNow(plugin(), $ -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin(), task);
        }
    }

    public static void runAsyncAfterOneSecond(@NotNull Runnable task) {
        if (FOLIA) {
            Bukkit.getAsyncScheduler().runDelayed(plugin(), $ -> task.run(), 1, TimeUnit.SECONDS);
        } else {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin(), task, 20);
        }
    }

    public static @NotNull Executor getRegionExecutor(@NotNull World world, int blockX, int blockZ) {
        if (FOLIA) {
            return command -> Bukkit.getRegionScheduler().run(plugin(), world, blockX >> 4, blockZ >> 4, $ -> command.run());
        } else {
            return Bukkit.getScheduler().getMainThreadExecutor(plugin());
        }
    }

    private static @NotNull Plugin plugin() {
        return JavaPlugin.getPlugin(RandomTPPlugin.class);
    }

    private Scheduler() {
        throw new UnsupportedOperationException();
    }
}
