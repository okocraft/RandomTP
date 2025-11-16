package net.okocraft.randomtp;

import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public class RandomTPPlugin extends JavaPlugin {

    private static final UUID NEXT_CLEANUP_UUID = new UUID(0, 0);

    private static final PotionEffect INVINCIBILITY = new PotionEffect(PotionEffectType.RESISTANCE, 100, 3);
    private final TranslationRegistry translationRegistry = BuiltinTranslations.createRegistry();

    private final Map<UUID, Instant> cooldownMap = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        GlobalTranslator.translator().addSource(translationRegistry);
    }

    @Override
    public void onDisable() {
        GlobalTranslator.translator().removeSource(translationRegistry);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(translatable("randomtp.command.not-player", RED));
            return true;
        }

        if (!sender.hasPermission("randomtp.self")) {
            sender.sendMessage(translatable().key("randomtp.no-permission").args(text("randomtp.self", AQUA)).color(RED));
            return true;
        }

        var cooldown = cooldownMap.get(player.getUniqueId());

        if (cooldown != null && !sender.hasPermission("randomtp.bypass-cooldown")) {
            var left = Duration.between(Instant.now(), cooldown);

            if (!(left.isNegative() || left.isZero())) {
                var remaining = translatable().key("randomtp.seconds").args(text(left.toSeconds())).color(AQUA);
                sender.sendMessage(translatable().key("randomtp.in-cooldown").args(remaining).color(GRAY));
                return true;
            }
        }

        if (CountdownTask.isTeleporting(player)) {
            sender.sendMessage(translatable("randomtp.already-teleporting", RED));
            return true;
        }

        var nextCleanup = cooldownMap.get(NEXT_CLEANUP_UUID);

        if (nextCleanup != null && Duration.between(Instant.now(), nextCleanup).isNegative()) {
            cleanupCooldownMap();
        }

        var world = player.getWorld();

        Scheduler.runAsync(() -> startRandomTeleport(player, world));

        return true;
    }

    private void startRandomTeleport(@NotNull Player player, @NotNull World world) {
        player.sendMessage(translatable("randomtp.generating-random-location", GRAY));

        int maxAttempts = 30;
        int attempts = 0;
        var random = new Random();

        Location safeLocation = null;

        while (attempts < maxAttempts) {
            safeLocation = LocationGenerator.generateSafeLocation(world, random);

            if (safeLocation != null) {
                break;
            }

            attempts++;
        }

        if (safeLocation == null) {
            player.sendMessage(translatable("randomtp.safe-location-not-found", RED));
            return;
        }

        var task = new CountdownTask(player, safeLocation, 5);

        task.onTeleport(this::startCooldown);
        task.onTeleport(this::playTeleportEffect);

        getServer().getPluginManager().registerEvents(task, this);
        Scheduler.runAsync(task);
    }

    private void startCooldown(@NotNull Player player) {
        if (!player.hasPermission("randomtp.bypass-cooldown")) {
            var cooldown = Instant.now().plusSeconds(300);
            cooldownMap.put(player.getUniqueId(), cooldown);
            cooldownMap.putIfAbsent(NEXT_CLEANUP_UUID, cooldown);
        }
    }

    private void playTeleportEffect(@NotNull Player player) {
        player.sendMessage(translatable("randomtp.teleported", GRAY));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.MASTER, 1.0f, 1.0f);
        player.spawnParticle(Particle.FIREWORK, player.getLocation(), 1);
        player.addPotionEffect(INVINCIBILITY);
    }

    private void cleanupCooldownMap() {
        var now = Instant.now();
        for (var uuid : Set.copyOf(cooldownMap.keySet())) {
            var cooldown = cooldownMap.get(uuid);
            if (cooldown != null && Duration.between(now, cooldown).isNegative()) {
                cooldownMap.remove(uuid);
            }
        }
    }
}
