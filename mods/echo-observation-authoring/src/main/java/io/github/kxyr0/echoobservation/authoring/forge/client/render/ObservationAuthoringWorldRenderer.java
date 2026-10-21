package io.github.kxyr0.echoobservation.authoring.forge.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.authoring.forge.client.ObservationAuthoringRuntime;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ObservationAuthoringWorldRenderer {
    private static final double HALF = 0.13D;
    private static final double SURFACE_OFFSET = 0.015D;
    private final ObservationAuthoringRuntime runtime;

    public ObservationAuthoringWorldRenderer(ObservationAuthoringRuntime runtime) {
        this.runtime = runtime;
    }

    @SubscribeEvent
    public void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!runtime.active() || minecraft.level == null
                || !rendersAt(event.getStage())) return;
        String dimension = minecraft.level.dimension().location().toString();
        var selected = runtime.selected().map(value -> value.id()).orElse(null);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderType gizmoType = ObservationAuthoringRenderTypes.gizmo();
        VertexConsumer vertices = buffers.getBuffer(gizmoType);
        Camera renderCamera = event.getCamera();
        PoseStack poses = worldView(renderCamera);
        Vec3 camera = renderCamera.getPosition();
        runtime.definitions().stream().filter(value -> value.anchor().isPresent())
                .filter(value -> value.anchor().orElseThrow().dimension().equals(dimension))
                .forEach(value -> marker(poses.last(), vertices, value.anchor().orElseThrow(), camera,
                        Objects.equals(selected, value.id()) ? 255 : 70,
                        Objects.equals(selected, value.id()) ? 211 : 210,
                        Objects.equals(selected, value.id()) ? 92 : 255));
        runtime.preview().filter(value -> value.dimension().equals(dimension))
                .ifPresent(value -> marker(poses.last(), vertices, value, camera, 255, 80, 210));
        buffers.endBatch(gizmoType);
    }

    private void marker(PoseStack.Pose pose, VertexConsumer vertices, ObservationAnchor anchor,
            Vec3 camera, int red, int green, int blue) {
        Vec3 center = visiblePoint(anchor, camera);
        Vec3 normal = new Vec3(anchor.face().normalX(), anchor.face().normalY(), anchor.face().normalZ());
        Vec3 tangent = Math.abs(normal.y) > 0.5D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 bitangent = normal.cross(tangent);
        Vec3 a = center.add(tangent.scale(HALF)).add(bitangent.scale(HALF));
        Vec3 b = center.add(tangent.scale(-HALF)).add(bitangent.scale(HALF));
        Vec3 c = center.add(tangent.scale(-HALF)).add(bitangent.scale(-HALF));
        Vec3 d = center.add(tangent.scale(HALF)).add(bitangent.scale(-HALF));
        line(pose, vertices, a, b, red, green, blue); line(pose, vertices, b, c, red, green, blue);
        line(pose, vertices, c, d, red, green, blue); line(pose, vertices, d, a, red, green, blue);
        line(pose, vertices, center, center.add(normal.scale(0.32D)), red, green, blue);
        line(pose, vertices, center.add(tangent.scale(-HALF)), center.add(tangent.scale(HALF)), red, green, blue);
        line(pose, vertices, center.add(bitangent.scale(-HALF)), center.add(bitangent.scale(HALF)), red, green, blue);
    }

    static boolean rendersAt(RenderLevelStageEvent.Stage stage) {
        return stage == RenderLevelStageEvent.Stage.AFTER_LEVEL;
    }

    private static PoseStack worldView(Camera camera) {
        PoseStack poses = new PoseStack();
        poses.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poses.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
        return poses;
    }

    static Vec3 renderPoint(ObservationAnchor value, Vec3 camera) {
        return new Vec3(value.worldX() - camera.x, value.worldY() - camera.y, value.worldZ() - camera.z);
    }

    static Vec3 visiblePoint(ObservationAnchor value, Vec3 camera) {
        return renderPoint(value, camera).add(
                value.face().normalX() * SURFACE_OFFSET,
                value.face().normalY() * SURFACE_OFFSET,
                value.face().normalZ() * SURFACE_OFFSET);
    }

    private void line(PoseStack.Pose pose, VertexConsumer vertices, Vec3 from, Vec3 to,
            int red, int green, int blue) {
        vertex(pose, vertices, from, red, green, blue);
        vertex(pose, vertices, to, red, green, blue);
    }

    private void vertex(PoseStack.Pose pose, VertexConsumer vertices, Vec3 point,
            int red, int green, int blue) {
        vertices.vertex(pose.pose(), (float) point.x, (float) point.y, (float) point.z)
                .color(red, green, blue, 235).normal(pose.normal(), 0.0F, 1.0F, 0.0F).endVertex();
    }
}
