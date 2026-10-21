package io.github.kxyr0.echoik.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kxyr0.echoanimation.forge.api.client.render.pose.AnimationHumanoidPoseOverlayEvent;
import io.github.kxyr0.echoik.core.math.Vec3;
import io.github.kxyr0.echoik.core.model.Pose;
import io.github.kxyr0.echoik.core.solver.IkTarget;
import net.minecraft.client.renderer.RenderType;

/**
 * Draws the minimal right-arm IK debug overlay in render-local coordinates.
 */
final class EchoIkDebugRenderer {
    private static final double SCALE = 1.0D / 16.0D;

    void render(AnimationHumanoidPoseOverlayEvent event, Pose pose, IkTarget target) {
        VertexConsumer vertices = event.frame().buffers().getBuffer(RenderType.lines());
        PoseStack.Pose current = event.frame().poses().last();
        Vec3 root = pose.requireTransform(target.rootBoneId()).translation();
        Vec3 mid = pose.requireTransform(target.midBoneId()).translation();
        Vec3 end = pose.requireTransform(target.endBoneId()).translation();
        line(vertices, current, root, mid, new Color(80, 220, 255));
        line(vertices, current, mid, end, new Color(80, 255, 120));
        point(vertices, current, target.targetPosition(), new Color(255, 60, 60));
        target.poleTarget().ifPresent(pole -> point(vertices, current, pole, new Color(255, 220, 60)));
    }

    private void point(VertexConsumer vertices, PoseStack.Pose pose, Vec3 center, Color color) {
        double size = 1.5D;
        line(vertices, pose, center.add(new Vec3(-size, 0.0D, 0.0D)),
                center.add(new Vec3(size, 0.0D, 0.0D)), color);
        line(vertices, pose, center.add(new Vec3(0.0D, -size, 0.0D)),
                center.add(new Vec3(0.0D, size, 0.0D)), color);
        line(vertices, pose, center.add(new Vec3(0.0D, 0.0D, -size)),
                center.add(new Vec3(0.0D, 0.0D, size)), color);
    }

    private void line(VertexConsumer vertices, PoseStack.Pose pose, Vec3 from, Vec3 to, Color color) {
        Vec3 normal = to.subtract(from).normalize();
        vertex(vertices, pose, from, normal, color);
        vertex(vertices, pose, to, normal, color);
    }

    private void vertex(VertexConsumer vertices, PoseStack.Pose pose, Vec3 position, Vec3 normal, Color color) {
        vertices.vertex(pose.pose(), (float) (position.x() * SCALE), (float) (position.y() * SCALE),
                (float) (position.z() * SCALE))
                .color(color.red(), color.green(), color.blue(), 255)
                .normal(pose.normal(), (float) normal.x(), (float) normal.y(), (float) normal.z())
                .endVertex();
    }

    private record Color(int red, int green, int blue) {
    }
}
