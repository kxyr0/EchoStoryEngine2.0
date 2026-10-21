package io.github.kxyr0.echostoryengine.blackspace.mixin;

import io.github.kxyr0.echostoryengine.blackspace.client.BlackspaceClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
abstract class BlackspaceEntityRenderDispatcherMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void echoBlackspace$forceLocalPlayerRender(
            E entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (isLocalPlayerInBlackspace(entity)) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void echoBlackspace$forceLocalPlayerFullBright(
            E entity,
            float partialTick,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (isLocalPlayerInBlackspace(entity)) {
            callback.setReturnValue(LightTexture.FULL_BRIGHT);
        }
    }

    private static boolean isLocalPlayerInBlackspace(Entity entity) {
        return BlackspaceClientState.isActive() && Minecraft.getInstance().player == entity;
    }
}
