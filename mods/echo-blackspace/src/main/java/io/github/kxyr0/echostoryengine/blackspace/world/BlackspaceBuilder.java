package io.github.kxyr0.echostoryengine.blackspace.world;

import io.github.kxyr0.echostoryengine.blackspace.api.BlackspaceInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;

final class BlackspaceBuilder {
    private static final BlockState BARRIER = Blocks.BARRIER.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    void build(ServerLevel level, BlackspaceInstance instance) {
        loadPlatformChunks(level, instance);
        placePlatform(level, instance);
        clearSpawnHeadroom(level, instance);
    }

    private void loadPlatformChunks(ServerLevel level, BlackspaceInstance instance) {
        BlockPos center = instance.platformCenter();
        int radius = instance.platformRadius();
        int minChunkX = SectionPos.blockToSectionCoord(center.getX() - radius);
        int maxChunkX = SectionPos.blockToSectionCoord(center.getX() + radius);
        int minChunkZ = SectionPos.blockToSectionCoord(center.getZ() - radius);
        int maxChunkZ = SectionPos.blockToSectionCoord(center.getZ() + radius);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
    }

    private void placePlatform(ServerLevel level, BlackspaceInstance instance) {
        BlockPos center = instance.platformCenter();
        int radius = instance.platformRadius();
        int y = center.getY();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                level.setBlock(new BlockPos(x, y, z), BARRIER, Block.UPDATE_ALL);
            }
        }
    }

    private void clearSpawnHeadroom(ServerLevel level, BlackspaceInstance instance) {
        BlockPos spawn = BlockPos.containing(instance.spawnX(), instance.spawnY(), instance.spawnZ());
        for (int yOffset = 0; yOffset <= 2; yOffset++) {
            for (int xOffset = -1; xOffset <= 1; xOffset++) {
                for (int zOffset = -1; zOffset <= 1; zOffset++) {
                    level.setBlock(spawn.offset(xOffset, yOffset, zOffset), AIR, Block.UPDATE_ALL);
                }
            }
        }
    }
}
