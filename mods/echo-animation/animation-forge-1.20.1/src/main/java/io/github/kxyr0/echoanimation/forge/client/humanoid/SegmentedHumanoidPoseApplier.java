package io.github.kxyr0.echoanimation.forge.client.humanoid;

import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.SegmentedHumanoidBone;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.rig.EvaluatedRigPose;
import io.github.kxyr0.echoanimation.core.rig.RigPoseEvaluator;
import net.minecraft.client.model.geom.ModelPart;

final class SegmentedHumanoidPoseApplier {
    private final RigPoseEvaluator poseEvaluator = new RigPoseEvaluator();

    void apply(ModelPart root, AnimationRigDefinition rig, AnimationPose pose, AnimationSkinVariant variant) {
        EvaluatedRigPose world = poseEvaluator.evaluate(rig, pose);
        EvaluatedRigPose defaults = poseEvaluator.evaluate(rig, AnimationPose.defaults(rig.bones()));
        PoseState state = new PoseState(world, defaults, pose, variant);

        ModelPart lowerBody = root.getChild("body_lower");
        applyTop(lowerBody, world, defaults, SegmentedHumanoidBone.BODY_LOWER,
                new PartBase(0.0F, 12.0F, 0.0F));

        ModelPart upperBody = lowerBody.getChild("body");
        applyLocal(upperBody, SegmentedHumanoidBone.BODY, pose,
                new PartBase(0.0F, -6.0F, 0.0F));
        applyLocal(upperBody.getChild("head"), SegmentedHumanoidBone.HEAD, pose,
                new PartBase(0.0F, -6.0F, 0.0F));

        applyArm(upperBody, false, state);
        applyArm(upperBody, true, state);
        applyLeg(root, false, world, defaults, pose);
        applyLeg(root, true, world, defaults, pose);
    }

    private void applyArm(ModelPart upperBody, boolean left, PoseState state) {
        SegmentedHumanoidBone shoulder = left ? SegmentedHumanoidBone.LEFT_SHOULDER
                : SegmentedHumanoidBone.RIGHT_SHOULDER;
        SegmentedHumanoidBone upper = left ? SegmentedHumanoidBone.LEFT_UPPER_ARM
                : SegmentedHumanoidBone.RIGHT_UPPER_ARM;
        SegmentedHumanoidBone forearm = left ? SegmentedHumanoidBone.LEFT_FOREARM
                : SegmentedHumanoidBone.RIGHT_FOREARM;
        ModelPart shoulderPart = upperBody.getChild(shoulder.id().value());
        applyLocal(shoulderPart, shoulder, state.pose(),
                new PartBase(left ? 5.0F : -5.0F,
                        state.variant() == AnimationSkinVariant.SLIM ? -3.5F : -4.0F, 0.0F));
        ModelPart upperPart = shoulderPart.getChild(left ? "left_upper_arm" : "right_upper_arm");
        applyLocal(upperPart, upper, state.pose(), PartBase.ZERO);
        ModelPart forearmPart = upperPart.getChild(left ? "left_forearm" : "right_forearm");
        applyLocal(forearmPart, forearm, state.pose(),
                new PartBase(0.0F, SegmentedHumanoidGeometry.FOREARM_PIVOT_Y, 0.0F));
    }

    private void applyLeg(ModelPart root, boolean left, EvaluatedRigPose world, EvaluatedRigPose defaults,
            AnimationPose pose) {
        SegmentedHumanoidBone thigh = left ? SegmentedHumanoidBone.LEFT_THIGH : SegmentedHumanoidBone.RIGHT_THIGH;
        SegmentedHumanoidBone shin = left ? SegmentedHumanoidBone.LEFT_SHIN : SegmentedHumanoidBone.RIGHT_SHIN;
        ModelPart thighPart = root.getChild(thigh.id().value());
        applyTop(thighPart, world, defaults, thigh, new PartBase(left ? 1.9F : -1.9F, 12.0F, 0.0F));
        ModelPart shinPart = thighPart.getChild(left ? "left_shin" : "right_shin");
        applyLocal(shinPart, shin, pose, new PartBase(0.0F, 6.0F, 0.0F));
    }

    private void applyTop(ModelPart part, EvaluatedRigPose world, EvaluatedRigPose defaults,
            SegmentedHumanoidBone bone, PartBase base) {
        apply(part, world.bone(bone.id()).orElseThrow(), defaults.bone(bone.id()).orElseThrow(), base);
    }

    private void applyLocal(ModelPart part, SegmentedHumanoidBone bone, AnimationPose pose, PartBase base) {
        EulerTransform defaults = bone.defaultTransform();
        apply(part, pose.transform(bone.id()).orElse(defaults), defaults, base);
    }

    private void apply(ModelPart part, EulerTransform actual, EulerTransform defaults, PartBase base) {
        part.x = base.x() + (float) (actual.translation().x() - defaults.translation().x());
        part.y = base.y() + (float) (actual.translation().y() - defaults.translation().y());
        part.z = base.z() + (float) (actual.translation().z() - defaults.translation().z());
        part.xRot = (float) Math.toRadians(actual.rotationDegrees().x());
        part.yRot = (float) Math.toRadians(actual.rotationDegrees().y());
        part.zRot = (float) Math.toRadians(actual.rotationDegrees().z());
        part.xScale = (float) actual.scale().x();
        part.yScale = (float) actual.scale().y();
        part.zScale = (float) actual.scale().z();
    }

    private record PartBase(float x, float y, float z) {
        private static final PartBase ZERO = new PartBase(0.0F, 0.0F, 0.0F);
    }

    private record PoseState(EvaluatedRigPose world, EvaluatedRigPose defaults, AnimationPose pose,
            AnimationSkinVariant variant) {
    }
}
