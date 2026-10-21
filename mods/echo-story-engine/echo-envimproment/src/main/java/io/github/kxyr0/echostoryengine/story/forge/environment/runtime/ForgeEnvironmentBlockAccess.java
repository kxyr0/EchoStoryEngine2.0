package io.github.kxyr0.echostoryengine.story.forge.environment.runtime;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.BlockPropertyDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentPosition;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Minecraft/Forge block access for EnvironmentService.
 */
public final class ForgeEnvironmentBlockAccess implements EnvironmentBlockAccess {
    private final ServerLevel level;

    public ForgeEnvironmentBlockAccess(ServerLevel level) {
        this.level = level;
    }

    @Override
    public NamespacedId dimensionId() {
        return NamespacedId.of(level.dimension().location().toString());
    }

    @Override
    public BlockStateView air() {
        return new ForgeState(Blocks.AIR.defaultBlockState());
    }

    @Override
    public Optional<BlockView> block(NamespacedId blockId) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(blockId.namespace(), blockId.path());
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        return block == null ? Optional.empty() : Optional.of(new ForgeBlock(blockId, block));
    }

    @Override
    public Optional<PreparedBlockEntityData> prepareBlockEntityData(
            BlockStateView state,
            Optional<String> nbt) throws EnvironmentApplyException {
        if (nbt.isEmpty()) {
            return Optional.empty();
        }
        ForgeState forgeState = forgeState(state);
        if (!(forgeState.state().getBlock() instanceof EntityBlock)) {
            throw new EnvironmentApplyException("Block entity NBT was provided for a block without BlockEntity.");
        }
        return Optional.of(new ForgeNbt(parseNbt(nbt.orElseThrow())));
    }

    @Override
    public void setBlock(
            EnvironmentPosition position,
            BlockStateView state,
            Optional<PreparedBlockEntityData> blockEntityData) throws EnvironmentApplyException {
        ForgeState forgeState = forgeState(state);
        BlockPos pos = new BlockPos(position.x(), position.y(), position.z());
        BlockState previous = level.getBlockState(pos);
        level.setBlock(pos, forgeState.state(), Block.UPDATE_ALL);
        if (blockEntityData.isPresent()) {
            loadBlockEntity(pos, previous, forgeState.state(), blockEntityData.orElseThrow());
        }
    }

    private void loadBlockEntity(
            BlockPos pos,
            BlockState previous,
            BlockState state,
            PreparedBlockEntityData data) throws EnvironmentApplyException {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || !(data instanceof ForgeNbt nbt)) {
            throw new EnvironmentApplyException("Placed block did not create the expected BlockEntity.");
        }
        CompoundTag tag = nbt.tag().copy();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        blockEntity.load(tag);
        blockEntity.setChanged();
        level.sendBlockUpdated(pos, previous, state, Block.UPDATE_ALL);
    }

    private CompoundTag parseNbt(String raw) throws EnvironmentApplyException {
        try {
            return TagParser.parseTag(raw);
        } catch (CommandSyntaxException exception) {
            throw new EnvironmentApplyException("Block entity NBT is invalid: " + exception.getMessage());
        }
    }

    private ForgeState forgeState(BlockStateView state) throws EnvironmentApplyException {
        if (state instanceof ForgeState forgeState) {
            return forgeState;
        }
        throw new EnvironmentApplyException("Block state adapter type is invalid.");
    }

    private record ForgeState(BlockState state) implements BlockStateView {
    }

    private record ForgeNbt(CompoundTag tag) implements PreparedBlockEntityData {
    }

    private record ForgeBlock(NamespacedId id, Block block) implements BlockView {
        @Override
        public BlockStateView defaultState() {
            return new ForgeState(block.defaultBlockState());
        }

        @Override
        public BlockStateView withProperty(
                BlockStateView state,
                BlockPropertyDef property) throws EnvironmentApplyException {
            ForgeState forgeState = (ForgeState) state;
            Property<?> blockProperty = block.getStateDefinition().getProperty(property.name());
            if (blockProperty == null) {
                throw new EnvironmentApplyException("Block '" + id.asString()
                        + "' does not contain property '" + property.name() + "'.");
            }
            return new ForgeState(applyProperty(forgeState.state(), blockProperty, property));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private BlockState applyProperty(
                BlockState state,
                Property property,
                BlockPropertyDef authored) throws EnvironmentApplyException {
            Optional value = property.getValue(authored.value());
            if (value.isEmpty()) {
                throw new EnvironmentApplyException("Property '" + authored.name()
                        + "' does not accept value '" + authored.value() + "'.");
            }
            return state.setValue(property, (Comparable) value.orElseThrow());
        }
    }
}
