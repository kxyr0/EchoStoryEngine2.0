package io.github.kxyr0.echostoryengine.blackspace.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echostoryengine.blackspace.client.BlackspaceClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
abstract class BlackspaceWorldLayerRendererMixin {
    @Inject(method = "isChunkCompiled", at = @At("HEAD"), cancellable = true)
    private void echoBlackspace$localPlayerChunkIsRenderable(
            BlockPos position,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (BlackspaceClientState.isActive()
                && Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.blockPosition().equals(position)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "renderChunkLayer", at = @At("HEAD"), cancellable = true)
    private void echoBlackspace$hideChunkLayer(
            RenderType renderType,
            PoseStack poseStack,
            double cameraX,
            double cameraY,
            double cameraZ,
            Matrix4f projection,
            CallbackInfo callback
    ) {
        if (BlackspaceClientState.isActive()) {
            callback.cancel();
        }
    }

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void echoBlackspace$hideWeather(
            LightTexture lightTexture,
            float partialTick,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfo callback
    ) {
        if (BlackspaceClientState.isActive()) {
            callback.cancel();
        }
    }
}
