package io.github.kxyr0.echoplayersleep.forge.bed;

import io.github.kxyr0.echoplayersleep.api.world.BlockId;
import io.github.kxyr0.echoplayersleep.api.world.BlockPosition;
import io.github.kxyr0.echoplayersleep.api.world.DimensionId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ForgeSleepIds {
    private ForgeSleepIds() {
    }

    public static DimensionId dimension(Level level) {
        ResourceKey<Level> key = level.dimension();
        return new DimensionId(key.location().toString());
    }

    public static BlockPosition position(BlockPos pos) {
        return new BlockPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    public static BlockPos blockPos(BlockPosition position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }

    public static BlockId block(BlockState state) {
        return new BlockId(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
    }
}
