package io.github.kxyr0.echofiremagic.forge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class BlacksmithBlankBlock extends Block {
    public static final BooleanProperty HEATED = BooleanProperty.create("heated");

    public BlacksmithBlankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HEATED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEATED);
    }

    public static int lightLevel(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(HEATED) ? 10 : 0;
    }
}
