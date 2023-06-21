package net.okocraft.randomtp;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public class CountdownTask implements Runnable, Listener {

    private static final Set<UUID> PLAYERS = Collections.synchronizedSet(new HashSet<>());

    public static boolean isTeleporting(@NotNull Player player) {
        return PLAYERS.contains(player.getUniqueId());
    }

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final Player player;
    private final Location location;
    private int remaining;
    private Consumer<Player> onTeleport;

    public CountdownTask(@NotNull Player player, @NotNull Location location, int start) {
        this.player = player;
        this.location = location;
        this.remaining = start + 1;

        PLAYERS.add(player.getUniqueId());
    }

    @Override
    public void run() {
        if (cancelled.get()) {
            PLAYERS.remove(player.getUniqueId());
            finishTask();
            return;
        }

        if (!player.getWorld().equals(location.getWorld())) {
            cancelled.set(true);
            player.sendMessage(translatable("randomtp.cancelled-due-to-change-world", RED));
            finishTask();
            return;
        }

        remaining--;

        if (remaining < 1) {
            finishTask();

            player.teleportAsync(location).thenAcceptAsync(success -> {
                if (success) {
                    if (onTeleport != null) {
                        Scheduler.runOnPlayerScheduler(player, onTeleport);
                    }
                } else {
                    player.sendMessage(translatable("randomtp.cannot-teleport", RED));
                }
            });
        } else {
            var seconds = translatable().key("randomtp.seconds").args(text(remaining)).color(AQUA);
            player.sendMessage(Component.translatable().key("randomtp.countdown").args(seconds).color(GRAY));

            Scheduler.runAsyncAfterOneSecond(this);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onMove(@NotNull PlayerMoveEvent event) {
        if (cancelled.get() || remaining < 1 || !event.getPlayer().equals(player)) {
            return;
        }

        Location loc1 = event.getFrom();
        Location loc2 = event.getTo();

        if (Math.round(loc1.getX()) != Math.round(loc2.getX()) ||
                Math.round(loc1.getY()) != Math.round(loc2.getY()) ||
                Math.round(loc1.getZ()) != Math.round(loc2.getZ())) {
            cancelled.set(true);
            player.sendMessage(Component.translatable("randomtp.cancelled-due-to-move", RED));
            finishTask();
        }
    }

    public void onTeleport(@NotNull Consumer<Player> playerConsumer) {
        if (this.onTeleport != null) {
            this.onTeleport = this.onTeleport.andThen(playerConsumer);
        } else {
            this.onTeleport = playerConsumer;
        }
    }

    private void finishTask() {
        PLAYERS.remove(player.getUniqueId());
        HandlerList.unregisterAll(this);
    }
}
