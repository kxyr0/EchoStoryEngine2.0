package io.github.kxyr0.echoconcentration.forge.client;

import io.github.kxyr0.echoconcentration.forge.client.shader.ShaderPackCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

final class ConcentrationPostEffect {
    private static final ResourceLocation DYNAMIC = effect("desaturate_dynamic");
    private static final int FADE_TICKS = 16;
    private PostChain ownedEffect;
    private float amount;
    private float centerX = 0.5F;
    private float centerY = 0.5F;

    void enter(Minecraft minecraft) {
        amount = 0.0F;
        if (!ShaderPackCompatibility.active()) load(minecraft);
    }

    void exit(Minecraft minecraft) {
        if (ownedEffect == null) release(minecraft);
    }

    void tick(Minecraft minecraft, boolean active) {
        if (ShaderPackCompatibility.active()) {
            release(minecraft);
            return;
        }
        if (active && (ownedEffect == null || minecraft.gameRenderer.currentEffect() != ownedEffect)) load(minecraft);
        float step = 1.0F / FADE_TICKS;
        amount = active ? Math.min(1.0F, amount + step) : Math.max(0.0F, amount - step);
        if (!active && amount <= 0.0F) {
            release(minecraft);
        } else {
            applyUniforms();
        }
    }

    void capturePlayer(Minecraft minecraft, RenderLevelStageEvent event) {
        centerX = 0.5F;
        centerY = 0.5F;
        if (minecraft.player != null && !minecraft.options.getCameraType().isFirstPerson()) {
            float partialTick = event.getPartialTick();
            Vec3 camera = event.getCamera().getPosition();
            double x = Mth.lerp(partialTick, minecraft.player.xo, minecraft.player.getX());
            double y = Mth.lerp(partialTick, minecraft.player.yo, minecraft.player.getY())
                    + minecraft.player.getBbHeight() * 0.5D;
            double z = Mth.lerp(partialTick, minecraft.player.zo, minecraft.player.getZ());
            Matrix4f worldToClip = new Matrix4f(event.getProjectionMatrix())
                    .mul(event.getPoseStack().last().pose());
            Vector4f clip = new Vector4f((float) (x - camera.x), (float) (y - camera.y),
                    (float) (z - camera.z), 1.0F).mul(worldToClip);
            if (clip.isFinite() && clip.w() > 0.001F) {
                centerX = Mth.clamp(clip.x() / clip.w() * 0.5F + 0.5F, 0.0F, 1.0F);
                centerY = Mth.clamp(clip.y() / clip.w() * 0.5F + 0.5F, 0.0F, 1.0F);
            }
        }
        applyUniforms();
    }

    void release(Minecraft minecraft) {
        if (ownedEffect != null && minecraft.gameRenderer.currentEffect() == ownedEffect) {
            minecraft.gameRenderer.shutdownEffect();
        }
        ownedEffect = null;
        amount = 0.0F;
    }

    private void load(Minecraft minecraft) {
        minecraft.gameRenderer.loadEffect(DYNAMIC);
        ownedEffect = minecraft.gameRenderer.currentEffect();
        applyUniforms();
    }

    private static ResourceLocation effect(String name) {
        return new ResourceLocation("echo_concentration", "shaders/post/" + name + ".json");
    }

    private void applyUniforms() {
        if (ownedEffect == null || ownedEffect.passes.isEmpty()) return;
        ownedEffect.passes.get(0).getEffect().safeGetUniform("Amount").set(amount);
        ownedEffect.passes.get(0).getEffect().safeGetUniform("Center").set(centerX, centerY);
    }
}
