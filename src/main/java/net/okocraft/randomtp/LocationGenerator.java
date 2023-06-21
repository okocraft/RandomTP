package net.okocraft.randomtp;

import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

final class LocationGenerator {

    // See https://github.com/EssentialsX/Essentials/blob/2.x/Essentials/src/main/java/com/earth2me/essentials/utils/LocationUtil.java#L24-L31
    private static final Set<Material> BAD_BLOCKS = EnumSet.of(
            Material.LAVA, Material.WATER, Material.CACTUS, Material.CAMPFIRE, Material.FIRE, Material.MAGMA_BLOCK,
            Material.SOUL_CAMPFIRE, Material.SOUL_FIRE, Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE,
            Material.END_PORTAL, Material.NETHER_PORTAL
    );

    public static @Nullable Location generateSafeLocation(@NotNull World world, @NotNull Random random) {
        int worldBorderRadius = ((int) world.getWorldBorder().getSize()) >> 1;

        int x = (random.nextBoolean() ? -1 : 1) * random.nextInt(worldBorderRadius);
        int z = (random.nextBoolean() ? -1 : 1) * random.nextInt(worldBorderRadius);

        var chunk = world.getChunkAtAsync(x >> 4, z >> 4).thenApply(c -> c.getChunkSnapshot(true, false, false)).join();

        int y = createSafeY(
                chunk,
                world.getEnvironment() == World.Environment.NETHER,
                x & 0xF,
                z & 0xF,
                world.getMinHeight(), world.getMaxHeight()
        );

        return y != Integer.MIN_VALUE ? new Location(world, x, y, z) : null;
    }

    private static int createSafeY(@NotNull ChunkSnapshot chunk, boolean isNether, int blockX, int blockZ, int minY, int maxY) {
        return isNether ?
                getSafeYInNether(chunk, blockX, blockZ) :
                getSafeHighestY(chunk, blockX, blockZ, minY, maxY);
    }

    private static int getSafeHighestY(ChunkSnapshot chunk, int blockX, int blockZ, int minY, int maxY) {
        int y = chunk.getHighestBlockYAt(blockX, blockZ) + 1;
        return y != minY && y + 1 != maxY && isLocationSafe(chunk, blockX, y, blockZ) ? y : Integer.MIN_VALUE;
    }

    private static int getSafeYInNether(ChunkSnapshot chunk, int blockX, int blockZ) {
        for (int y = 32; y < 100; y++) { // y < 32 can be lava, and 100 < y is basically a ceiling
            if (isLocationSafe(chunk, blockX, y, blockZ)) {
                return y;
            }
        }

        return Integer.MIN_VALUE;
    }

    private static boolean isLocationSafe(@NotNull ChunkSnapshot chunk, int blockX, int y, int blockZ) {
        var block = chunk.getBlockType(blockX, y, blockZ);
        var below = chunk.getBlockType(blockX, y - 1, blockZ);
        var above = chunk.getBlockType(blockX, y + 1, blockZ);

        // overworld or the end
        return !BAD_BLOCKS.contains(above)
                && !BAD_BLOCKS.contains(block)
                && !BAD_BLOCKS.contains(below)
                && !above.isSolid()
                && !block.isSolid()
                && below.isSolid();
    }

    private LocationGenerator() {
        throw new UnsupportedOperationException();
    }
}
