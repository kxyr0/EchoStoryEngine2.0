package io.github.kxyr0.echotrigger.forge.provider.payload;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockPayloads {
    private BlockPayloads() {
    }

    public static Map<String, String> block(Level level, BlockPos pos, BlockState state) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("block", ForgeIds.block(state));
        payload.put("target", ForgeIds.block(state));
        payload.put("tags", ForgeIds.blockTags(state));
        payload.put("blockTags", ForgeIds.blockTags(state));
        payload.put("dimension", ForgeIds.dimension(level));
        payload.put("position", ForgeIds.position(pos));
        return payload;
    }

    public static Map<String, String> interaction(
            Level level,
            BlockPos pos,
            BlockState state,
            InteractionHand hand
    ) {
        Map<String, String> payload = new LinkedHashMap<>(block(level, pos, state));
        payload.put("hand", hand == null ? "ANY_HAND" : ForgeIds.hand(hand));
        return payload;
    }
}
