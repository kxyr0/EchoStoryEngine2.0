package io.github.kxyr0.echoanimation.forge.client.skinning.render.gpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;

import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** Draws deformed vertices through the active vanilla or Iris/Oculus entity shader. */
public final class ShaderPackEntityDrawer {
    private static final float PIXEL = 1.0F / 16.0F;
    private final ShaderPackEntityState entityState = new ShaderPackEntityState();

    public void draw(GpuSkinningMesh mesh, AnimationHumanoidRenderContext context) {
        RenderType renderType = RenderType.entityCutoutNoCull(context.skinTexture());
        renderType.setupRenderState();
        ShaderInstance shader = RenderSystem.getShader();
        if (shader == null) {
            renderType.clearRenderState();
            throw new IllegalStateException("Active entity shader is missing");
        }
        try {
            Matrix4f modelView = new Matrix4f(context.poses().last().pose()).scale(PIXEL);
            prepare(shader, modelView, RenderSystem.getProjectionMatrix());
            mesh.bindForEntityShader( context.overlay(), context.packedLight(), entityState.currentEntityId());
            shader.apply();
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, mesh.vertexCount());
        } finally {
            GL30.glBindVertexArray(0);
            shader.clear();
            renderType.clearRenderState();
            BufferUploader.invalidate();
        }
    }

    private void prepare(ShaderInstance shader, Matrix4f modelView, Matrix4f projection) {
        for (int index = 0; index < 12; index++) {
            shader.setSampler("Sampler" + index, RenderSystem.getShaderTexture(index));
        }
        set(shader.MODEL_VIEW_MATRIX, modelView);
        set(shader.PROJECTION_MATRIX, projection);
        if (shader.INVERSE_VIEW_ROTATION_MATRIX != null) {
            shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
        }
        if (shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }
        if (shader.GLINT_ALPHA != null) {
            shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }
        prepareFog(shader);
        if (shader.TEXTURE_MATRIX != null) {
            shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }
        if (shader.GAME_TIME != null) {
            shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }
        if (shader.SCREEN_SIZE != null) {
            var window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
        }
        RenderSystem.setupShaderLights(shader);
    }

    private void prepareFog(ShaderInstance shader) {
        if (shader.FOG_START != null) {
            shader.FOG_START.set(RenderSystem.getShaderFogStart());
        }
        if (shader.FOG_END != null) {
            shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        }
        if (shader.FOG_COLOR != null) {
            shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }
        if (shader.FOG_SHAPE != null) {
            shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
        }
    }

    private void set(com.mojang.blaze3d.shaders.Uniform uniform, Matrix4f value) {
        if (uniform != null) {
            uniform.set(value);
        }
    }
}
