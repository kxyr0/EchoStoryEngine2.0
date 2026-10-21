package io.github.kxyr0.echodebugvisibilityadapter.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kxyr0.echodebugvisibilityadapter.forge.client.VisibilityDebugAccess;
import io.github.kxyr0.echodebugvisibilityadapter.forge.client.VisibilityDebugSettings;
import io.github.kxyr0.echodebugvisibilityadapter.forge.client.VisibilityFrustumDebugGeometry;
import io.github.kxyr0.echovisibility.api.debug.VisibilityDebugSample;
import io.github.kxyr0.echovisibility.api.debug.VisibilityDebugSnapshot;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityAabb;
import io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class VisibilityWorldRenderer {
    private static final double MARKER_SIZE = 0.08D;
    private final VisibilityDebugSettings settings;
    private final VisibilityDebugAccess access;
    private final VisibilityFrustumDebugGeometry frustum = new VisibilityFrustumDebugGeometry();

    public VisibilityWorldRenderer(VisibilityDebugSettings settings, VisibilityDebugAccess access) {
        this.settings = settings;
        this.access = access;
    }

    @SubscribeEvent
    public void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!settings.active() || minecraft.level == null
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        access.view().ifPresent(view -> {
            MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            VertexConsumer vertices = buffers.getBuffer(RenderType.lines());
            PoseStack poses = event.getPoseStack();
            var renderCamera = event.getCamera().getPosition();
            poses.pushPose();
            poses.translate(-renderCamera.x, -renderCamera.y, -renderCamera.z);
            view.camera().filter(camera -> settings.player()).ifPresent(camera ->
                    drawCamera(poses.last(), vertices, camera));
            access.watches(view, settings).forEach(snapshot ->
                    drawWatch(poses.last(), vertices, snapshot));
            poses.popPose();
            buffers.endBatch(RenderType.lines());
        });
    }

    private void drawCamera(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            VisibilityCameraSnapshot camera) {
        line(pose, vertices, camera.playerEyePosition(),
                camera.playerEyePosition().add(camera.playerForward().scale(4.0D)), 75, 235, 115);
        line(pose, vertices, camera.position(),
                camera.position().add(camera.forward().scale(4.0D)), 55, 205, 255);
        for (var segment : frustum.segments(camera)) {
            line(pose, vertices, segment.from(), segment.to(), 55, 205, 255);
        }
    }

    private static void drawWatch(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            VisibilityDebugSnapshot snapshot) {
        snapshot.targetBounds().ifPresent(bounds -> box(pose, vertices, bounds.bounds(), 255, 214, 64));
        snapshot.camera().ifPresent(camera -> snapshot.samples().forEach(sample -> {
            int[] color = color(sample);
            line(pose, vertices, camera.position(), sample.position(), color[0], color[1], color[2]);
            marker(pose, vertices, sample.position(), color[0], color[1], color[2]);
            sample.hitPosition().ifPresent(hit -> marker(pose, vertices, hit, 255, 70, 70));
        }));
    }

    private static int[] color(VisibilityDebugSample sample) {
        return switch (sample.rayStatus()) {
            case "CLEAR" -> new int[] {55, 235, 110};
            case "BLOCKED" -> new int[] {255, 70, 70};
            default -> new int[] {255, 170, 55};
        };
    }

    private static void marker(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            VisibilityVector3 point,
            int red,
            int green,
            int blue) {
        line(pose, vertices, point.add(new VisibilityVector3(-MARKER_SIZE, 0, 0)),
                point.add(new VisibilityVector3(MARKER_SIZE, 0, 0)), red, green, blue);
        line(pose, vertices, point.add(new VisibilityVector3(0, -MARKER_SIZE, 0)),
                point.add(new VisibilityVector3(0, MARKER_SIZE, 0)), red, green, blue);
        line(pose, vertices, point.add(new VisibilityVector3(0, 0, -MARKER_SIZE)),
                point.add(new VisibilityVector3(0, 0, MARKER_SIZE)), red, green, blue);
    }

    private static void box(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            VisibilityAabb box,
            int red,
            int green,
            int blue) {
        VisibilityVector3 min = box.min();
        VisibilityVector3 max = box.max();
        VisibilityVector3[] points = {
            new VisibilityVector3(min.x(), min.y(), min.z()), new VisibilityVector3(max.x(), min.y(), min.z()),
            new VisibilityVector3(max.x(), min.y(), max.z()), new VisibilityVector3(min.x(), min.y(), max.z()),
            new VisibilityVector3(min.x(), max.y(), min.z()), new VisibilityVector3(max.x(), max.y(), min.z()),
            new VisibilityVector3(max.x(), max.y(), max.z()), new VisibilityVector3(min.x(), max.y(), max.z())
        };
        int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6},
            {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        for (int[] edge : edges) {
            line(pose, vertices, points[edge[0]], points[edge[1]], red, green, blue);
        }
    }

    private static void line(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            VisibilityVector3 from,
            VisibilityVector3 to,
            int red,
            int green,
            int blue) {
        vertex(pose, vertices, from, red, green, blue);
        vertex(pose, vertices, to, red, green, blue);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            VisibilityVector3 point,
            int red,
            int green,
            int blue) {
        vertices.vertex(pose.pose(), (float) point.x(), (float) point.y(), (float) point.z())
                .color(red, green, blue, 210).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }
}
