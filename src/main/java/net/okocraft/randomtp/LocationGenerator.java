package net.okocraft.randomtp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

final class LocationGenerator {

    // See https://github.com/EssentialsX/Essentials/blob/2.x/Essentials/src/main/java/com/earth2me/essentials/utils/LocationUtil.java#L24-L31
    private static final Set<Material> BAD_BLOCKS = EnumSet.of(
            Material.LAVA, Material.WATER, Material.CACTUS, Material.CAMPFIRE, Material.FIRE, Material.MAGMA_BLOCK,
            Material.SOUL_CAMPFIRE, Material.SOUL_FIRE, Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE,
            Material.END_PORTAL, Material.NETHER_PORTAL
    );

    public static @NotNull CompletableFuture<@Nullable Location> generateSafeLocation(@NotNull World world, @NotNull Random random) {
        int worldBorderRadius = ((int) world.getWorldBorder().getSize()) >> 1;
        int x = (random.nextBoolean() ? -1 : 1) * random.nextInt(worldBorderRadius);
        int z = (random.nextBoolean() ? -1 : 1) * random.nextInt(worldBorderRadius);

        return CompletableFuture.supplyAsync(
                () -> {
                    var loc = createLocationWithSafeY(world, x, z);
                    return loc != null && isLocationSafe(loc) ? loc : null;
                },
                Scheduler.getRegionExecutor(world, x, z)
        );
    }

    private static @Nullable Location createLocationWithSafeY(@NotNull World world, int x, int z) {
        int y;

        if (world.getEnvironment() == World.Environment.NETHER) {
            y = getSafeYInNether(world, x, z);
        } else {
            y = getHighestY(world, x, z);
        }

        if (y == Integer.MIN_VALUE) { // No safe location in nether
            return null;
        }

        return new Location(world, x, y, z);
    }

    private static boolean isLocationSafe(@NotNull Location location) {
        return isLocationSafe(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static boolean isLocationSafe(@NotNull World world, int x, int y, int z) {
        if (!world.isChunkGenerated(x >> 4, z >> 4)) {
            return false;
        }

        var block = world.getBlockAt(x, y, z).getType();
        var below = world.getBlockAt(x, y - 1, z).getType();
        var above = world.getBlockAt(x, y + 1, z).getType();

        // overworld or the end
        return !BAD_BLOCKS.contains(above)
                && !BAD_BLOCKS.contains(block)
                && !BAD_BLOCKS.contains(below)
                && !above.isSolid()
                && !block.isSolid()
                && below.isSolid();
    }

    private static int getHighestY(World world, int x, int z) {
        return world.getHighestBlockYAt(x, z) + 1;
    }

    private static int getSafeYInNether(World world, int x, int z) {
        for (int y = 32; y < 100; y++) { // y < 32 can be lava, and 100 < y is basically a ceiling
            if (isLocationSafe(world, x, y, z)) {
                return y;
            }
        }

        return Integer.MIN_VALUE;
    }

    private LocationGenerator() {
        throw new UnsupportedOperationException();
    }
}
