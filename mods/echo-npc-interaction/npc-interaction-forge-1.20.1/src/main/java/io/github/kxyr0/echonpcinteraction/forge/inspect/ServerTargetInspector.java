package io.github.kxyr0.echonpcinteraction.forge.inspect;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpcinteraction.api.target.NpcInteractionTarget;
import io.github.kxyr0.echonpcinteraction.api.target.NpcInteractionTargetSnapshot;
import io.github.kxyr0.echonpcinteraction.forge.api.target.NpcInteractionTargetView;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public final class ServerTargetInspector {
    public Optional<NpcInteractionTargetView> inspect(MinecraftServer server, NpcInteractionTarget target) {
        Optional<ServerLevel> level = level(server, target.dimensionId());
        if (level.isEmpty()) {
            return Optional.empty();
        }
        BlockPos pos = new BlockPos(target.position().x(), target.position().y(), target.position().z());
        BlockState state = level.orElseThrow().getBlockState(pos);
        if (state.isAir()) {
            return Optional.empty();
        }
        String blockId = String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
        var snapshot = new NpcInteractionTargetSnapshot(target.dimensionId(), target.position(),
                new BlockId(blockId), BlockStateFingerprint.state(state),
                BlockStateFingerprint.collision(level.orElseThrow(), pos, state),
                state.hashCode(), level.orElseThrow().getGameTime());
        return Optional.of(new NpcInteractionTargetView(snapshot, properties(level.orElseThrow(), pos, state)));
    }

    private Optional<ServerLevel> level(MinecraftServer server, String dimensionId) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimensionId));
        return Optional.ofNullable(server.getLevel(key));
    }

    private Map<String, String> properties(ServerLevel level, BlockPos pos, BlockState state) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("blockId", String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock())));
        values.put("hasBlockEntity", Boolean.toString(level.getBlockEntity(pos) != null));
        values.put("state", state.toString());
        return Map.copyOf(values);
    }
}
