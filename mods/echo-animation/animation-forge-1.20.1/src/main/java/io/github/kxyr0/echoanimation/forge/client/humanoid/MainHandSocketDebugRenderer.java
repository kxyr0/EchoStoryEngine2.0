package io.github.kxyr0.echoanimation.forge.client.humanoid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.rig.RigPoseEvaluator;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import net.minecraft.client.renderer.RenderType;

final class MainHandSocketDebugRenderer {
    private static final AnimationAttachmentSocketId MAIN_HAND = AnimationAttachmentSocketId.parse("main_hand");
    private static final String PROPERTY = "echo_animation.debug.main_hand_socket_axes";
    private static final float AXIS_LENGTH = 0.25F;
    private final RigPoseEvaluator evaluator = new RigPoseEvaluator();

    void render(AnimationHumanoidRenderContext context, AnimationRigDefinition rig, AnimationPose pose) {
        if (!Boolean.getBoolean(PROPERTY)) {
            return;
        }
        evaluator.evaluate(rig, pose).socket(MAIN_HAND).ifPresent(socket -> renderAxes(context, socket));
    }

    private void renderAxes(AnimationHumanoidRenderContext context, EulerTransform socket) {
        context.poses().pushPose();
        context.poses().translate(socket.translation().x() / 16.0D,
                socket.translation().y() / 16.0D, socket.translation().z() / 16.0D);
        context.poses().mulPose(Axis.XP.rotationDegrees((float) socket.rotationDegrees().x()));
        context.poses().mulPose(Axis.YP.rotationDegrees((float) socket.rotationDegrees().y()));
        context.poses().mulPose(Axis.ZP.rotationDegrees((float) socket.rotationDegrees().z()));
        VertexConsumer vertices = context.buffers().getBuffer(RenderType.lines());
        PoseStack.Pose current = context.poses().last();
        axis(vertices, current, new LineVector(AXIS_LENGTH, 0.0F, 0.0F), new Rgb(255, 0, 0));
        axis(vertices, current, new LineVector(0.0F, AXIS_LENGTH, 0.0F), new Rgb(0, 255, 0));
        axis(vertices, current, new LineVector(0.0F, 0.0F, AXIS_LENGTH), new Rgb(0, 96, 255));
        context.poses().popPose();
    }

    private void axis(VertexConsumer vertices, PoseStack.Pose pose, LineVector direction, Rgb color) {
        vertex(vertices, pose, LineVector.ZERO, direction, color);
        vertex(vertices, pose, direction, direction, color);
    }

    private void vertex(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            LineVector position,
            LineVector normal,
            Rgb color
    ) {
        vertices.vertex(pose.pose(), position.x(), position.y(), position.z())
                .color(color.red(), color.green(), color.blue(), 255)
                .normal(pose.normal(), normal.x(), normal.y(), normal.z())
                .endVertex();
    }

    private record LineVector(float x, float y, float z) {
        private static final LineVector ZERO = new LineVector(0.0F, 0.0F, 0.0F);
    }

    private record Rgb(int red, int green, int blue) {
    }
}
