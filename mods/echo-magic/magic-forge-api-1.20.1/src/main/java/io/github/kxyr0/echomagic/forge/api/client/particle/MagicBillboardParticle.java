package io.github.kxyr0.echomagic.forge.api.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;

public final class MagicBillboardParticle extends TextureSheetParticle {
    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override public void begin(BufferBuilder buffer, TextureManager textures) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }
        @Override public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
        @Override public String toString() { return "ECHO_MAGIC_ADDITIVE"; }
    };

    private final float initialSize;
    private final float initialAlpha;
    private final float finalAlpha;

    MagicBillboardParticle(ClientLevel level, double x, double y, double z, MagicBillboardParticleOptions options) {
        super(level, x, y, z);
        setColor(options.red(), options.green(), options.blue());
        quadSize = options.size();
        initialSize = options.size();
        alpha = options.alphaStart();
        initialAlpha = options.alphaStart();
        finalAlpha = options.alphaEnd();
        lifetime = options.lifetime();
        hasPhysics = false;
        gravity = 0.0F;
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (++age >= lifetime) { remove(); return; }
        move(xd, yd, zd);
        float progress = age / (float) lifetime;
        float smooth = progress * progress * (3.0F - 2.0F * progress);
        alpha = initialAlpha + (finalAlpha - initialAlpha) * smooth;
        quadSize = initialSize * (1.0F - 0.25F * smooth);
    }
    @Override public int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ADDITIVE; }
    @Override public void render(com.mojang.blaze3d.vertex.VertexConsumer consumer, Camera camera, float partialTick) {
        super.render(consumer, camera, partialTick);
    }
}
