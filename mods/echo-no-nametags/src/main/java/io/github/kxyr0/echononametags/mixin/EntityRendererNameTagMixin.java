package io.github.kxyr0.echononametags.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Blocks the actual draw call, including direct calls that bypass Forge's name-tag event. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererNameTagMixin {
    @Inject(method = {"renderNameTag", "m_7649_"}, at = @At("HEAD"),
            cancellable = true, remap = false, require = 1)
    private void echoNoNameTags$hideNameTag(CallbackInfo callback) {
        callback.cancel();
    }
}
