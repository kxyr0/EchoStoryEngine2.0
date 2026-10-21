package io.github.kxyr0.echoanimation.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.client.rendering.FaceLayerDepth;
import io.github.kxyr0.echoanimation.client.rendering.LayerDrawCommand;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws authored face layers as textured quads on the front of the humanoid head. */
final class FaceLayerQuadRenderer {
    private static final float PIXEL = 1.0F / 16.0F;
    private static final float FACE_CENTER_Y = -4.0F * PIXEL;
    private static final float FACE_SURFACE_Z = -4.0F * PIXEL;
    private static final float FACE_SURFACE_OFFSET = 0.03F * PIXEL;
    private static final float FACE_Z = FACE_SURFACE_Z - FACE_SURFACE_OFFSET;

    private final FaceLayerTextureCache textureCache;

    FaceLayerQuadRenderer(FaceLayerTextureCache textureCache) {
        this.textureCache = textureCache;
    }

    void render( PoseStack poses, MultiBufferSource buffers, int light, LayerDrawCommand command,
            Transform3 placement) {
        ResourceLocation texture = textureLocation(command.textureId().value());
        if (texture == null) {
            return;
        }
        FaceLayerTextureCache.TextureSize size = textureCache.size(texture);
        IntRect rect = command.atlasRect();
        float u0 = rect.x() / (float) size.width();
        float v0 = rect.y() / (float) size.height();
        float u1 = Math.min(rect.x() + rect.width(), size.width()) / (float) size.width();
        float v1 = Math.min(rect.y() + rect.height(), size.height()) / (float) size.height();
        draw( poses, buffers, light, placement, new Quad( command.layerId().value(), command.role(), texture,
                        rect.width() * PIXEL, rect.height() * PIXEL, u0, v0, u1, v1));
    }

    private ResourceLocation textureLocation(String value) {
        return FaceLayerTextureLocations.clientTexture(value);
    }

    private void draw( PoseStack poses, MultiBufferSource buffers, int light, Transform3 placement, Quad quad) {
        VertexContext context = new VertexContext( buffers.getBuffer(renderType(quad.role(), quad.texture())),
                        poses.last().pose(), poses.last().normal(), light);
        double z = resolvedFaceZ(quad.role(), quad.layerId(), placement);
        Vec3 a = point(placement, -quad.width() / 2.0D, -quad.height() / 2.0D, z);
        Vec3 b = point(placement, -quad.width() / 2.0D, quad.height() / 2.0D, z);
        Vec3 c = point(placement, quad.width() / 2.0D, quad.height() / 2.0D, z);
        Vec3 d = point(placement, quad.width() / 2.0D, -quad.height() / 2.0D, z);
        vertex(context, a, quad.u0(), quad.v0());
        vertex(context, b, quad.u0(), quad.v1());
        vertex(context, c, quad.u1(), quad.v1());
        vertex(context, d, quad.u1(), quad.v0());
    }

    static double resolvedFaceZ(Transform3 placement) {
        return resolvedFaceZ(LayerRole.DECORATION, "sclera", placement);
    }

    static double resolvedFaceZ(LayerRole role, String layerId, Transform3 placement) {
        return FaceLayerDepth.resolve(FACE_Z, role, layerId, placement.translation().z());
    }

    private RenderType renderType(LayerRole role, ResourceLocation texture) {
        if (role == LayerRole.GLASSES) {
            return RenderType.entityTranslucent(texture);
        }
        return RenderType.entityCutoutNoCull(texture);
    }

    private Vec3 point(Transform3 placement, double x, double y, double z) {
        Transform3 facePlacement = new Transform3(
                        new Vec3(placement.translation().x(), placement.translation().y(), 0.0D),
                        placement.rotationZDegrees(), placement.scale(), placement.pivot());
        Vec3 moved = facePlacement.applyPoint(new Vec3(x, y, 0.0D));
        return new Vec3(moved.x(), FACE_CENTER_Y + moved.y(), z);
    }

    private void vertex(VertexContext context, Vec3 point, float u, float v) {
        context.consumer() .vertex(context.matrix(), (float) point.x(), (float) point.y(), (float) point.z())
                .color(255, 255, 255, 255) .uv(u, v) .overlayCoords(OverlayTexture.NO_OVERLAY) .uv2(context.light())
                .normal(context.normal(), 0.0F, 0.0F, -1.0F) .endVertex();
    }

    private record Quad( String layerId, LayerRole role, ResourceLocation texture, float width, float height, float u0,
            float v0, float u1, float v1) {}

    private record VertexContext( VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int light) {}
}
