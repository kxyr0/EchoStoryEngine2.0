package io.github.kxyr0.echoanimation.forge.client.humanoid;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.client.humanoid.SegmentedHumanoidGeometry.Box;
import io.github.kxyr0.echoanimation.forge.client.humanoid.SegmentedHumanoidGeometry.Uv;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;

/** Draws split humanoid cuboids with vanilla player-skin UVs. */
final class SegmentedHumanoidCuboidRenderer {
    void render(ModelPart root, AnimationHumanoidRenderContext frame) {
        VertexConsumer vertices = frame.buffers().getBuffer(RenderType.entityCutoutNoCull(frame.skinTexture()));
        RenderContext context = new RenderContext(vertices, frame.packedLight(), frame.overlay());
        renderHead(root, frame.poses(), context);
        renderBody(root, frame.poses(), context);
        renderArm(root, frame.poses(), context, frame.skinVariant(), false);
        renderArm(root, frame.poses(), context, frame.skinVariant(), true);
        renderLeg(root, frame.poses(), context, false);
        renderLeg(root, frame.poses(), context, true);
    }

    private void renderHead(ModelPart root, PoseStack poses, RenderContext context) {
        ModelPart lowerBody = root.getChild("body_lower");
        ModelPart upperBody = lowerBody.getChild("body");
        ModelPart head = upperBody.getChild("head");
        poses.pushPose();
        lowerBody.translateAndRotate(poses);
        upperBody.translateAndRotate(poses);
        renderPart( head, poses, context, new Box(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                Uv.visible(0, 0, new SegmentedHumanoidGeometry.Dimensions(8, 8, 8), 0, 8));
        renderPart( head, poses, context, new Box(-4.25F, -8.25F, -4.25F, 8.5F, 8.5F, 8.5F),
                Uv.visible(32, 0, new SegmentedHumanoidGeometry.Dimensions(8, 8, 8), 0, 8));
        poses.popPose();
    }

    private void renderBody(ModelPart root, PoseStack poses, RenderContext context) {
        ModelPart lowerBody = root.getChild("body_lower");
        ModelPart upperBody = lowerBody.getChild("body");

        renderPart( lowerBody, poses, context, new Box(-4.0F, -6.0F, -2.0F, 8.0F, 6.0F, 4.0F),
                Uv.visible(16, 16, new SegmentedHumanoidGeometry.Dimensions(8, 12, 4), 6, 6).withTopCut());
        renderPart( lowerBody, poses, context, new Box(-4.25F, -6.25F, -2.25F, 8.5F, 6.5F, 4.5F),
                Uv.visible(16, 32, new SegmentedHumanoidGeometry.Dimensions(8, 12, 4), 6, 6).withTopCut());

        poses.pushPose();
        lowerBody.translateAndRotate(poses);
        renderPart( upperBody, poses, context, new Box(-4.0F, -6.0F, -2.0F, 8.0F, 6.0F, 4.0F),
                Uv.visible(16, 16, new SegmentedHumanoidGeometry.Dimensions(8, 12, 4), 0, 6).withBottomCut());
        renderPart( upperBody, poses, context, new Box(-4.25F, -6.25F, -2.25F, 8.5F, 6.5F, 4.5F),
                Uv.visible(16, 32, new SegmentedHumanoidGeometry.Dimensions(8, 12, 4), 0, 6).withBottomCut());
        poses.popPose();
    }

    private void renderArm( ModelPart root, PoseStack poses, RenderContext context, AnimationSkinVariant variant,
            boolean left) {
        String side = left ? "left" : "right";
        ModelPart lowerBody = root.getChild("body_lower");
        ModelPart upperBody = lowerBody.getChild("body");
        ModelPart shoulder = upperBody.getChild(side + "_shoulder");
        ModelPart upper = shoulder.getChild(side + "_upper_arm");
        ModelPart forearm = upper.getChild(side + "_forearm");
        float width = variant == AnimationSkinVariant.SLIM ? 3.0F : 4.0F;
        float minX = left ? -1.0F : -width + 1.0F;
        int baseX = left ? 32 : 40;
        int baseY = left ? 48 : 16;
        int overlayX = left ? 48 : 40;
        int overlayY = left ? 48 : 32;
        poses.pushPose();
        lowerBody.translateAndRotate(poses);
        upperBody.translateAndRotate(poses);
        shoulder.translateAndRotate(poses);
        var layout = new ArmLayout(width, minX, baseX, baseY, overlayX, overlayY);
        renderUpperArm(upper, poses, context, layout);
        upper.translateAndRotate(poses);
        renderForearm(forearm, poses, context, layout);
        poses.popPose();
    }

    private record ArmLayout(float width, float minX, int baseX, int baseY, int overlayX, int overlayY) { }
    private void renderUpperArm(ModelPart upper, PoseStack poses, RenderContext context, ArmLayout layout) {
        renderPart( upper, poses, context, new Box( layout.minX(), SegmentedHumanoidGeometry.ARM_TOP_Y, -2.0F,
                layout.width(),
                        SegmentedHumanoidGeometry.UPPER_ARM_HEIGHT, 4.0F), Uv.visible(layout.baseX(),
                layout.baseY(), new
                SegmentedHumanoidGeometry.Dimensions(layout.width(), 12, 4),
                SegmentedHumanoidGeometry.UPPER_ARM_SOURCE_Y,
                SegmentedHumanoidGeometry.UPPER_ARM_HEIGHT) .withBottomCut());
        renderPart( upper, poses, context, new Box( layout.minX() - SegmentedHumanoidGeometry.OVERLAY_INFLATE,
                        SegmentedHumanoidGeometry.ARM_TOP_Y
                                - SegmentedHumanoidGeometry.OVERLAY_INFLATE,
                        -2.0F - SegmentedHumanoidGeometry.OVERLAY_INFLATE,
                        layout.width() + SegmentedHumanoidGeometry.OVERLAY_INFLATE * 2.0F,
                        SegmentedHumanoidGeometry.UPPER_ARM_HEIGHT + SegmentedHumanoidGeometry.OVERLAY_INFLATE * 2.0F,
                        4.0F + SegmentedHumanoidGeometry.OVERLAY_INFLATE * 2.0F), Uv.visible(layout.overlayX(),
                layout.overlayY(),
                new SegmentedHumanoidGeometry.Dimensions(layout.width(), 12, 4),
                SegmentedHumanoidGeometry.UPPER_ARM_SOURCE_Y, SegmentedHumanoidGeometry.UPPER_ARM_HEIGHT)
                .withBottomCut());
    }

    private void renderForearm(ModelPart forearm, PoseStack poses, RenderContext context, ArmLayout layout) {
        renderPart( forearm, poses, context, new Box( layout.minX(), SegmentedHumanoidGeometry.FOREARM_GEOMETRY_Y,
                -2.0F, layout.width(),
                        SegmentedHumanoidGeometry.FOREARM_HEIGHT, 4.0F), Uv.visible(layout.baseX(), layout.baseY(), new
                SegmentedHumanoidGeometry.Dimensions(layout.width(), 12, 4), SegmentedHumanoidGeometry.FOREARM_SOURCE_Y,
                SegmentedHumanoidGeometry.FOREARM_HEIGHT)
                        .withTopCut());
        renderPart( forearm, poses, context, new Box( layout.minX() - SegmentedHumanoidGeometry.OVERLAY_INFLATE,
                        SegmentedHumanoidGeometry.FOREARM_GEOMETRY_Y
                                - SegmentedHumanoidGeometry.OVERLAY_INFLATE,
                        -2.0F - SegmentedHumanoidGeometry.OVERLAY_INFLATE,
                        layout.width() + SegmentedHumanoidGeometry.OVERLAY_INFLATE * 2.0F,
                        SegmentedHumanoidGeometry.FOREARM_HEIGHT + SegmentedHumanoidGeometry.OVERLAY_INFLATE * 2.0F,
                        4.0F + SegmentedHumanoidGeometry.OVERLAY_INFLATE * 2.0F), Uv.visible(layout.overlayX(),
                layout.overlayY(),
                new SegmentedHumanoidGeometry.Dimensions(layout.width(), 12, 4),
                SegmentedHumanoidGeometry.FOREARM_SOURCE_Y,
                SegmentedHumanoidGeometry.FOREARM_HEIGHT) .withTopCut());
    }

    private void renderLeg(ModelPart root, PoseStack poses, RenderContext context, boolean left) {
        String side = left ? "left" : "right";
        ModelPart thigh = root.getChild(side + "_thigh");
        ModelPart shin = thigh.getChild(side + "_shin");
        int baseX = left ? 16 : 0;
        int baseY = left ? 48 : 16;
        int overlayX = 0;
        int overlayY = left ? 48 : 32;
        poses.pushPose();
        renderPart( thigh, poses, context, new Box(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                Uv.visible(baseX, baseY, new SegmentedHumanoidGeometry.Dimensions(4, 12, 4), 0, 6).withBottomCut());
        renderPart( thigh, poses, context, new Box(-2.25F, -0.25F, -2.25F, 4.5F, 6.5F, 4.5F),
                Uv.visible(overlayX, overlayY, new SegmentedHumanoidGeometry.Dimensions(4, 12, 4), 0,
                6).withBottomCut());
        thigh.translateAndRotate(poses);
        renderPart( shin, poses, context, new Box(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                Uv.visible(baseX, baseY, new SegmentedHumanoidGeometry.Dimensions(4, 12, 4), 6, 6).withTopCut());
        renderPart( shin, poses, context, new Box(-2.25F, -0.25F, -2.25F, 4.5F, 6.5F, 4.5F),
                Uv.visible(overlayX, overlayY, new SegmentedHumanoidGeometry.Dimensions(4, 12, 4), 6, 6).withTopCut());
        poses.popPose();
    }

    private void renderPart( ModelPart part, PoseStack poses, RenderContext context, Box box, Uv uv) {
        poses.pushPose();
        part.translateAndRotate(poses);
        SegmentedHumanoidGeometry.drawBox( new SegmentedHumanoidGeometry.Draw(
                        poses.last(), context.vertices(), context.light(), context.overlay()), box, uv);
        poses.popPose();
    }

    private record RenderContext(VertexConsumer vertices, int light, int overlay) {}
}
