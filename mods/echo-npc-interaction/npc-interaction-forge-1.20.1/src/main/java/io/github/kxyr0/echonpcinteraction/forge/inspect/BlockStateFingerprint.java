package io.github.kxyr0.echonpcinteraction.forge.inspect;

import java.util.StringJoiner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

public final class BlockStateFingerprint {
    private BlockStateFingerprint() {
    }

    public static String state(BlockState state) {
        String id = String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
        return id + "|" + state.getValues();
    }

    public static String collision(ServerLevel level, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty() ? "empty" : shape.bounds().toString();
    }

    public static String neighborhood(ServerLevel level, BlockPos center) {
        StringJoiner states = new StringJoiner(";");
        for (int dy = -4; dy <= 4; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    addObservedState(level, center, states, dx, dy, dz);
                }
            }
        }
        return states.toString();
    }

    private static void addObservedState(ServerLevel level, BlockPos center, StringJoiner states,
            int dx, int dy, int dz) {
        BlockPos pos = center.offset(dx, dy, dz);
        if (level.isOutsideBuildHeight(pos) || !level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return;
        }
        states.add(dx + "," + dy + "," + dz + "=" + state(level.getBlockState(pos)));
    }
}
