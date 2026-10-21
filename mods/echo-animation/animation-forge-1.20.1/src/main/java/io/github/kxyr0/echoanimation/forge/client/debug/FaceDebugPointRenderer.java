package io.github.kxyr0.echoanimation.forge.client.debug;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import io.github.kxyr0.echoanimation.core.transform.TransformEvaluator;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import io.github.kxyr0.echoanimation.forge.debug.FaceDebugState;
import io.github.kxyr0.echoanimation.client.rendering.FaceLayerPoseProjector;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Draws authored face origin and pivot points over the rendered face plane.
 */
public final class FaceDebugPointRenderer {
    private static final double PIXEL = 1.0D / 16.0D;
    private static final double FACE_CENTER_Y = -4.0D * PIXEL;
    private static final double FACE_Z = -4.12D * PIXEL;
    private static final double DEPTH_SCALE = 0.01D;
    private static final double CROSS = 0.012D;
    private static final int BONE_ORIGIN = 0xFF24D8FF;
    private static final int BONE_PIVOT = 0xFFFF4FD8;
    private static final int LAYER_ORIGIN = 0xFFFFD34D;
    private static final int LAYER_PIVOT = 0xFF5CFF7A;

    private final TransformEvaluator evaluator = new TransformEvaluator();
    private final FaceLayerPoseProjector projector = new FaceLayerPoseProjector();

    public void render(PoseStack poses, MultiBufferSource buffers, int light, FaceRigDefinition rig, BonePose pose) {
        if (!FaceDebugState.enabled()) {
            return;
        }
        List<FaceDebugPoint> screenPoints = new ArrayList<>();
        var world = evaluator.evaluate(rig.hierarchy(), pose);
        VertexContext context = new VertexContext(buffers.getBuffer(RenderType.lines()),
                poses.last().pose(), poses.last().normal(), light);
        rig.hierarchy().bones().forEach(bone -> world.get(bone.id()).ifPresent(transform -> {
            addTransformPoints(context, screenPoints, "bone " + bone.id().value(), transform, true);
        }));
        projector.project(rig, pose, java.util.Set.of()).forEach(layer -> addTransformPoints(context, screenPoints,
                "layer " + layer.command().layerId().value() + " " + layer.command().role().name().toLowerCase(),
                layer.transform(), false));
        FaceDebugOverlay.add(screenPoints);
    }

    private void addTransformPoints(VertexContext context, List<FaceDebugPoint> screenPoints, String label,
            Transform3 transform, boolean bone) {
        if (FaceDebugState.origins()) {
            addPoint(context, screenPoints, label + " origin", transform.translation(),
                    bone ? BONE_ORIGIN : LAYER_ORIGIN);
        }
        if (FaceDebugState.pivots() && !zero(transform.pivot())) {
            addPoint(context, screenPoints, label + " pivot", transform.applyPoint(transform.pivot()),
                    bone ? BONE_PIVOT : LAYER_PIVOT);
        }
    }

    private void addPoint(VertexContext context, List<FaceDebugPoint> screenPoints, String label, Vec3 face,
            int color) {
        Vec3 model = modelPoint(face);
        drawCross(context, model, color);
        screen(context, model).ifPresent(point -> screenPoints.add(new FaceDebugPoint(label, point.x(), point.y(),
                color)));
    }

    private Vec3 modelPoint(Vec3 face) {
        double z = FACE_Z - Math.max(face.z(), 0.0D) * DEPTH_SCALE;
        return new Vec3(face.x(), FACE_CENTER_Y + face.y(), z);
    }

    private Optional<ScreenPoint> screen(VertexContext context, Vec3 point) {
        Vector4f clip = new Vector4f((float) point.x(), (float) point.y(), (float) point.z(), 1.0F);
        clip.mul(context.matrix());
        clip.mul(RenderSystem.getProjectionMatrix());
        if (clip.w() <= 0.0F) {
            return Optional.empty();
        }
        double ndcX = clip.x() / clip.w();
        double ndcY = clip.y() / clip.w();
        if (Math.abs(ndcX) > 1.2D || Math.abs(ndcY) > 1.2D) {
            return Optional.empty();
        }
        var window = Minecraft.getInstance().getWindow();
        int x = (int) Math.round((ndcX * 0.5D + 0.5D) * window.getGuiScaledWidth());
        int y = (int) Math.round((0.5D - ndcY * 0.5D) * window.getGuiScaledHeight());
        return Optional.of(new ScreenPoint(x, y));
    }

    private void drawCross(VertexContext context, Vec3 center, int color) {
        line(context, center.plus(new Vec3(-CROSS, 0.0D, 0.0D)),
                center.plus(new Vec3(CROSS, 0.0D, 0.0D)), color);
        line(context, center.plus(new Vec3(0.0D, -CROSS, 0.0D)),
                center.plus(new Vec3(0.0D, CROSS, 0.0D)), color);
    }

    private void line(VertexContext context, Vec3 a, Vec3 b, int color) {
        vertex(context, a, color);
        vertex(context, b, color);
    }

    private void vertex(VertexContext context, Vec3 point, int color) {
        context.consumer().vertex(context.matrix(), (float) point.x(), (float) point.y(), (float) point.z())
                .color(red(color), green(color), blue(color), alpha(color))
                .normal(context.normal(), 0.0F, 0.0F, -1.0F)
                .endVertex();
    }

    private boolean zero(Vec3 value) {
        return value.x() == 0.0D && value.y() == 0.0D && value.z() == 0.0D;
    }

    private int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    private int red(int color) {
        return (color >>> 16) & 0xFF;
    }

    private int green(int color) {
        return (color >>> 8) & 0xFF;
    }

    private int blue(int color) {
        return color & 0xFF;
    }

    private record VertexContext(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int light) {
    }

    private record ScreenPoint(int x, int y) {
    }
}
