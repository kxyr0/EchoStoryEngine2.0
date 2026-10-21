package io.github.kxyr0.echocompatibility.worldedit.mixin;

import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import io.github.kxyr0.echocompatibility.worldedit.EchoCompatibilityWorldEditMod;
import io.github.kxyr0.echocompatibility.worldedit.StateCountEstimator;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.sk89q.worldedit.extension.platform.Capability$4", remap = false)
abstract class WorldEditBlockStatePrewarmMixin {
    @Inject(
            method = "ready(Lcom/sk89q/worldedit/extension/platform/PlatformManager;"
                    + "Lcom/sk89q/worldedit/extension/platform/Platform;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void echoCompatibility$boundedPrewarm(
            PlatformManager manager,
            Platform platform,
            CallbackInfo callback) {
        callback.cancel();
        BlockRegistry registry = platform.getRegistries().getBlockRegistry();
        long limit = EchoCompatibilityWorldEditMod.maxStatesPerBlock();
        int registeredStates = 0;
        int skippedBlocks = 0;

        for (BlockType blockType : BlockType.REGISTRY) {
            StateCountEstimator.Estimate estimate = estimate(blockType, limit);
            if (estimate.exceedsLimit()) {
                skippedBlocks++;
                EchoCompatibilityWorldEditMod.LOGGER.warn(
                        "Skipped WorldEdit state prewarm for {}: estimated states={} exceeds limit={}.",
                        blockType.getId(), estimate.count(), limit);
                continue;
            }
            EchoCompatibilityWorldEditMod.LOGGER.debug(
                    "Prewarming WorldEdit states for {} (estimatedStates={}).",
                    blockType.getId(), estimate.count());
            try {
                for (BlockState state : blockType.getAllStates()) {
                    OptionalInt nativeId = registry.getInternalBlockStateId(state);
                    BlockStateIdAccess.register(state, nativeId.orElse(BlockStateIdAccess.invalidId()));
                    registeredStates++;
                }
            } catch (RuntimeException exception) {
                skippedBlocks++;
                EchoCompatibilityWorldEditMod.LOGGER.warn(
                        "Skipped WorldEdit state prewarm for {} after a compatibility error.",
                        blockType.getId(), exception);
            }
        }

        EchoCompatibilityWorldEditMod.LOGGER.info(
                "WorldEdit bounded state prewarm complete: registeredStates={}, skippedBlocks={}, limit={}.",
                registeredStates, skippedBlocks, limit);
    }

    private static StateCountEstimator.Estimate estimate(BlockType blockType, long limit) {
        List<Integer> sizes = new ArrayList<>();
        for (Property<?> property : blockType.getProperties()) {
            sizes.add(property.getValues().size());
        }
        return StateCountEstimator.estimate(sizes, limit);
    }
}
