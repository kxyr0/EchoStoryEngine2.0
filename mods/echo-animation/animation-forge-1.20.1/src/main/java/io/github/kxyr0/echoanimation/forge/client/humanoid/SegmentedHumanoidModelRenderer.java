package io.github.kxyr0.echoanimation.forge.client.humanoid;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.forge.api.client.render.AnimationHumanoidRenderContext;
import io.github.kxyr0.echoanimation.forge.api.client.render.pose.AnimationHumanoidPoseOverlayEvent;
import io.github.kxyr0.echoanimation.forge.api.client.render.pose.AnimationHumanoidPoseRenderFrame;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Quaternionf;

/**
 * Generic segmented 64x64 Minecraft-skin humanoid renderer.
 */
public final class SegmentedHumanoidModelRenderer {
    private static final float PIXEL = 1.0F / 16.0F;

    private final Map<AnimationSkinVariant, ModelPart> roots = new EnumMap<>(AnimationSkinVariant.class);
    private final SegmentedHumanoidPoseApplier poseApplier = new SegmentedHumanoidPoseApplier();
    private final SegmentedHumanoidCuboidRenderer cuboidRenderer = new SegmentedHumanoidCuboidRenderer();
    private final MainHandSocketDebugRenderer socketDebugRenderer = new MainHandSocketDebugRenderer();

    public void render(AnimationHumanoidRenderContext context) {
        ModelPart root = roots.computeIfAbsent(context.skinVariant(), this::bake);
        root.getAllParts().forEach(ModelPart::resetPose);
        cuboidRenderer.render(root, context);
    }

    public void render(AnimationHumanoidRenderContext context, AnimationRigDefinition rig, AnimationPose pose) {
        ModelPart root = roots.computeIfAbsent(context.skinVariant(), this::bake);
        AnimationPose renderPose = overlayPose(context, rig, pose);
        poseApplier.apply(root, rig, renderPose, context.skinVariant());
        cuboidRenderer.render(root, context);
        renderAttachments(root, context);
        socketDebugRenderer.render(context, rig, renderPose);
    }

    public ModelPart head(AnimationSkinVariant variant) {
        return upperBody(roots.computeIfAbsent(variant, this::bake)).getChild("head");
    }

    public void applyHeadTransform(AnimationSkinVariant variant, PoseStack poses) {
        ModelPart root = roots.computeIfAbsent(variant, this::bake);
        ModelPart lowerBody = lowerBody(root);
        ModelPart upperBody = upperBody(root);
        lowerBody.translateAndRotate(poses);
        upperBody.translateAndRotate(poses);
        upperBody.getChild("head").translateAndRotate(poses);
    }

    private ModelPart bake(AnimationSkinVariant variant) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition lowerBody = root.addOrReplaceChild("body_lower", empty(),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition upperBody = lowerBody.addOrReplaceChild("body", empty(),
                PartPose.offset(0.0F, -6.0F, 0.0F));
        upperBody.addOrReplaceChild("head", empty(), PartPose.offset(0.0F, -6.0F, 0.0F));
        addArm(upperBody, "right_shoulder", false, variant);
        addArm(upperBody, "left_shoulder", true, variant);
        addLeg(root, "right_thigh", false);
        addLeg(root, "left_thigh", true);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }

    private AnimationPose overlayPose(
            AnimationHumanoidRenderContext context,
            AnimationRigDefinition rig,
            AnimationPose pose
    ) {
        AnimationHumanoidPoseOverlayEvent event = new AnimationHumanoidPoseOverlayEvent(
                context.subjectId(), rig, pose.transforms(), new AnimationHumanoidPoseRenderFrame(
                context.poses(), context.buffers(), context.packedLight(), context.partialTick()));
        MinecraftForge.EVENT_BUS.post(event);
        return event.overlayTransforms().map(AnimationPose::new).orElse(pose);
    }

    private void addArm(PartDefinition upperBody, String shoulderName, boolean left, AnimationSkinVariant variant) {
        float x = left ? 5.0F : -5.0F;
        float y = variant == AnimationSkinVariant.SLIM ? -3.5F : -4.0F;
        PartDefinition shoulder = upperBody.addOrReplaceChild(shoulderName, empty(),
                PartPose.offset(x, y, 0.0F));
        PartDefinition upper = shoulder.addOrReplaceChild(upperArmName(left), empty(), PartPose.ZERO);
        upper.addOrReplaceChild(forearmName(left), empty(),
                PartPose.offset(0.0F, SegmentedHumanoidGeometry.FOREARM_PIVOT_Y, 0.0F));
    }

    private void addLeg(PartDefinition root, String thighName, boolean left) {
        float x = left ? 1.9F : -1.9F;
        PartDefinition thigh = root.addOrReplaceChild(thighName, empty(),
                PartPose.offset(x, 12.0F, 0.0F));
        thigh.addOrReplaceChild(shinName(left), empty(),
                PartPose.offset(0.0F, 6.0F, 0.0F));
    }

    private CubeListBuilder empty() {
        return CubeListBuilder.create();
    }

    // Sitting is no longer hardcoded here. HumanoidRenderDispatcher samples
    // jdh:pose_sit_main as a real system base pose.

    private void renderAttachments(
            ModelPart root,
            AnimationHumanoidRenderContext context) {
        ModelPart lowerBody = lowerBody(root);
        ModelPart upperBody = upperBody(root);

        context.attachments().render((boneName, poses) -> {
            switch (boneName) {
                case "body_lower" ->
                        applyRelativePartTransform(lowerBody, new PartBase(0.0F, 12.0F, 0.0F), poses);

                case "body" -> {
                    applyRelativePartTransform(lowerBody, new PartBase(0.0F, 12.0F, 0.0F), poses);
                    applyRelativePartTransform(upperBody, new PartBase(0.0F, -6.0F, 0.0F), poses);
                }

                case "left_shoulder" -> {
                    applyRelativePartTransform(lowerBody, new PartBase(0.0F, 12.0F, 0.0F), poses);
                    applyRelativePartTransform(upperBody, new PartBase(0.0F, -6.0F, 0.0F), poses);
                    applyRelativePartTransform(upperBody.getChild("left_shoulder"),
                            shoulderBase(true, context.skinVariant()), poses);
                }

                case "right_shoulder" -> {
                    applyRelativePartTransform(lowerBody, new PartBase(0.0F, 12.0F, 0.0F), poses);
                    applyRelativePartTransform(upperBody, new PartBase(0.0F, -6.0F, 0.0F), poses);
                    applyRelativePartTransform(upperBody.getChild("right_shoulder"),
                            shoulderBase(false, context.skinVariant()), poses);
                }

                default -> {
                    // Unknown names stay untouched for legacy attachment integrations.
                }
            }
        });
    }

    private PartBase shoulderBase(boolean left, AnimationSkinVariant variant) {
        return new PartBase(
                left ? 5.0F : -5.0F,
                variant == AnimationSkinVariant.SLIM ? -3.5F : -4.0F,
                0.0F);
    }

    private ModelPart lowerBody(ModelPart root) {
        return root.getChild("body_lower");
    }

    private ModelPart upperBody(ModelPart root) {
        return lowerBody(root).getChild("body");
    }

    private void applyRelativePartTransform(
            ModelPart part,
            PartBase base,
            PoseStack poses) {

        poses.translate(
                (part.x - base.x()) * PIXEL,
                (part.y - base.y()) * PIXEL,
                (part.z - base.z()) * PIXEL);

        if (part.xRot != 0.0F
                || part.yRot != 0.0F
                || part.zRot != 0.0F) {
            poses.mulPose(
                    new Quaternionf().rotationZYX(
                            part.zRot,
                            part.yRot,
                            part.xRot));
        }

        if (part.xScale != 1.0F
                || part.yScale != 1.0F
                || part.zScale != 1.0F) {
            poses.scale(
                    part.xScale,
                    part.yScale,
                    part.zScale);
        }
    }

    private String upperArmName(boolean left) {
        return left ? "left_upper_arm" : "right_upper_arm";
    }

    private String forearmName(boolean left) {
        return left ? "left_forearm" : "right_forearm";
    }

    private String shinName(boolean left) {
        return left ? "left_shin" : "right_shin";
    }

    private record PartBase(float x, float y, float z) {
        private static final PartBase ZERO = new PartBase(0.0F, 0.0F, 0.0F);
    }
}
