package io.github.kxyr0.echoanimation.forge.client.skinning.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedMesh;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedModel;
import io.github.kxyr0.echoanimation.forge.client.skinning.model.SkinnedVertex;
import io.github.kxyr0.echoanimation.forge.client.skinning.pose.CpuSkinningMath;
import io.github.kxyr0.echoanimation.forge.debug.SkinningDebugState;

import net.minecraft.client.renderer.RenderType;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Explicit test/debug fallback; production rendering remains GPU-backed. */
final class CpuSkinningBackend implements SkinningBackend {
    private static final float PIXEL = 1.0F / 16.0F;
    private static final Matrix4f[] NO_PALETTE = new Matrix4f[0];
    private final CpuSkinningMath math = new CpuSkinningMath();

    @Override
    public void render( SkinnedModel model, Matrix4f[] palette, AnimationHumanoidRenderContext context, int debugMode) {
        draw(model, palette, context, false);
    }

    void renderRaw(SkinnedModel model, AnimationHumanoidRenderContext context) {
        draw(model, NO_PALETTE, context, true);
    }

    private void draw( SkinnedModel model, Matrix4f[] palette, AnimationHumanoidRenderContext context, boolean raw) {
        VertexConsumer consumer = context.buffers().getBuffer(RenderType.entityCutoutNoCull(context.skinTexture()));
        PoseStack.Pose pose = context.poses().last();
        for (SkinnedMesh mesh : model.meshes()) {
            int[] triangles = mesh.triangles();
            for (int offset = 0; offset < triangles.length; offset += 3) {
                for (int corner = 0; corner < 3; corner++) {
                    vertex( new Draw(consumer, pose, context, model), mesh.vertices().get(triangles[offset + corner]),
                            palette, raw);
                }
                vertex( new Draw(consumer, pose, context, model), mesh.vertices().get(triangles[offset + 2]), palette,
                        raw);
            }
        }
    }

    private record Draw( VertexConsumer consumer, PoseStack.Pose pose, AnimationHumanoidRenderContext context,
            SkinnedModel model) {}

    private void vertex(Draw draw, SkinnedVertex vertex, Matrix4f[] palette, boolean raw) {
        var consumer = draw.consumer();
        var pose = draw.pose();
        var context = draw.context();
        var model = draw.model();
        Vector3f position = raw ? new Vector3f(vertex.x(), vertex.y(), vertex.z()) : math.position(vertex, palette);
        Vector3f normal = raw ? new Vector3f(vertex.normalX(), vertex.normalY(), vertex.normalZ())
                        : math.normal(vertex, palette);
        float renderOffsetY = SkinningDebugState.targets(context.subjectId())
                                && SkinningDebugState.mode() == SkinningDebugState.Mode.THERIG_BIND
                        ? SkinningDebugState.therigOffsetY().orElse(model.renderOffsetY()) : model.renderOffsetY();
        position.add(model.renderOffsetX(), renderOffsetY, model.renderOffsetZ());
        consumer.vertex( pose.pose(), position.x() * PIXEL, position.y() * PIXEL, position.z() * PIXEL)
                .color(255, 255, 255, 255) .uv(vertex.u(), vertex.v()) .overlayCoords(context.overlay())
                .uv2(context.packedLight()) .normal(pose.normal(), normal.x(), normal.y(), normal.z()) .endVertex();
    }
}
