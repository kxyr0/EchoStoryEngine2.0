package io.github.kxyr0.echostoryengine.blackspace.mixin;

import io.github.kxyr0.echostoryengine.blackspace.client.BlackspaceClientState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
abstract class BlackspaceCameraMixin {
    private static final double DETACHED_EYE_DISTANCE_SQ = 0.25D;

    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true)
    private void echoBlackspace$detachedWhenCameraLeavesPlayer(CallbackInfoReturnable<Boolean> callback) {
        if (!BlackspaceClientState.isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Camera camera = (Camera) (Object) this;
        if (player != null
                && camera.getEntity() == player
                && camera.getPosition().distanceToSqr(player.getEyePosition(minecraft.getFrameTime()))
                        > DETACHED_EYE_DISTANCE_SQ) {
            callback.setReturnValue(true);
        }
    }
}
