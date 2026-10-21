package io.github.kxyr0.echofiremagic.forge.blacksmith;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

record BlacksmithHeatJob(UUID playerId, ResourceKey<Level> dimension, long dueTick, Target target) {
    sealed interface Target permits ItemTarget, BlockTarget { }

    record ItemTarget(InteractionHand hand, ItemStack expected) implements Target {
        ItemTarget {
            expected = expected.copy();
            expected.setCount(1);
        }
    }

    record BlockTarget(BlockPos pos, Block expected) implements Target {
        BlockTarget { pos = pos.immutable(); }
    }
}
