package io.github.kxyr0.echoteleportmagic.forge.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoteleportmagic.forge.glow.TeleportBodyEmission;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Explicit official/SRG aliases are verified against Forge 47.4.10; no runtime reflection. */
@Mixin(EntityRenderDispatcher.class)
public abstract class TeleportEntityRenderMixin {
    @Inject(method = {"render", "m_114384_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void echoTeleportPreviewHide(Entity entity, double x, double y, double z, float yaw, float partial,
            PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo ci) {
        var visual = io.github.kxyr0.echoteleportmagic.forge.client.TeleportClient.subject(entity.getUUID());
        if (visual != null && visual.packet.preview() && !visual.packet.arrival()
                && visual.time(partial) >= 1.95 && visual.time(partial) < 4) ci.cancel();
    }
    @ModifyVariable(method = {"render", "m_114384_"}, at = @At("HEAD"), argsOnly = true, remap = false)
    private MultiBufferSource echoTeleportEmission(MultiBufferSource original, Entity entity,
            double x, double y, double z, float yaw, float partial, PoseStack poses,
            MultiBufferSource buffers, int light) {
        return TeleportBodyEmission.wrap(entity, original, partial);
    }
    @Inject(method = {"render", "m_114384_"}, at = @At("RETURN"), remap = false)
    private void echoTeleportFinish(Entity entity, double x, double y, double z, float yaw, float partial,
            PoseStack poses, MultiBufferSource buffers, int light, CallbackInfo ci) {
        TeleportBodyEmission.finish(buffers);
    }
}
