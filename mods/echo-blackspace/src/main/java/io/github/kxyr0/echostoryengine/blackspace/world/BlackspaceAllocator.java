package io.github.kxyr0.echostoryengine.blackspace.world;

import io.github.kxyr0.echostoryengine.blackspace.api.BlackspaceInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

final class BlackspaceAllocator {
    static final int PLATFORM_RADIUS = 8;
    private static final int BLACKSPACE_X = 65_536;
    private static final int BLACKSPACE_Z = 65_536;
    private static final int Y_OFFSET_FROM_TOP = 32;
    private static final double VISUAL_RADIUS = 96.0D;
    private static final double VERTICAL_RADIUS = 96.0D;

    BlackspaceInstance allocate(ServerLevel level, String id, int attempt) {
        int y = Math.max(level.getMinBuildHeight() + 8, level.getMaxBuildHeight() - Y_OFFSET_FROM_TOP);
        BlockPos center = new BlockPos(BLACKSPACE_X, y, BLACKSPACE_Z);
        return new BlackspaceInstance(
                id,
                level.dimension(),
                center,
                BLACKSPACE_X + 0.5D,
                y + 1.0D,
                BLACKSPACE_Z + 0.5D,
                180.0F,
                0.0F,
                PLATFORM_RADIUS,
                VISUAL_RADIUS,
                VERTICAL_RADIUS);
    }
}
