package io.github.kxyr0.echoanimation.core;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec2;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.AnimationEndBehavior;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import io.github.kxyr0.echoanimation.core.control.FaceControlSampler;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchy;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneHierarchyValidator;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import io.github.kxyr0.echoanimation.core.pose.BoneTrack;
import io.github.kxyr0.echoanimation.core.pose.ClipSampler;
import io.github.kxyr0.echoanimation.core.pose.Keyframe;
import io.github.kxyr0.echoanimation.core.transform.TransformEvaluator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class CoreRequirementMatrixTest {
    private final BoneId root = BoneId.parse("face_root");
    private final BoneId child = BoneId.parse("upper_eyelids");
    private final BoneId grandchild = BoneId.parse("lower_eyelids");
    private final FaceControlSampler controls = new FaceControlSampler();

    @Test
    void rootBoneAccepted() {
        var hierarchy = new BoneHierarchy(List.of(BoneDefinition.root(root)));
        Assertions.assertEquals(root, hierarchy.find(root).orElseThrow().id());
    }

    @Test
    void parentInheritanceIsStored() {
        var hierarchy = new BoneHierarchy(List.of(BoneDefinition.root(root), bone(child, root, Transform3.IDENTITY)));
        Assertions.assertEquals(Optional.of(root), hierarchy.find(child).orElseThrow().parentId());
    }

    @Test
    void selfParentRejected() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new BoneDefinition(root, Optional.of(root), Transform3.IDENTITY));
    }

    @Test
    void directCycleRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BoneHierarchy(List.of(
                bone(root, child, Transform3.IDENTITY), bone(child, root, Transform3.IDENTITY))));
    }

    @Test
    void indirectCycleRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BoneHierarchy(List.of(
                bone(root, grandchild, Transform3.IDENTITY),
                bone(child, root, Transform3.IDENTITY),
                bone(grandchild, child, Transform3.IDENTITY))));
    }

    @Test
    void depthBoundEnforced() {
        var bones = List.of(BoneDefinition.root(root), bone(child, root, Transform3.IDENTITY),
                bone(grandchild, child, Transform3.IDENTITY), bone(BoneId.parse("pupil_left"), grandchild,
                        Transform3.IDENTITY));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BoneHierarchyValidator(2).validate(bones));
    }

    @Test
    void parentRotationAffectsChild() {
        Transform3 parent = new Transform3(Vec3.ZERO, 90.0D, Vec3.ONE, Vec3.ZERO);
        Transform3 local = Transform3.IDENTITY.withTranslation(new Vec3(1.0D, 0.0D, 0.0D));
        Transform3 world = evaluate(parent, local);
        Assertions.assertEquals(0.0D, world.translation().x(), 0.0001D);
        Assertions.assertEquals(1.0D, world.translation().y(), 0.0001D);
    }

    @Test
    void parentScaleAffectsChild() {
        Transform3 parent = new Transform3(Vec3.ZERO, 0.0D, new Vec3(2.0D, 3.0D, 1.0D), Vec3.ZERO);
        Transform3 local = Transform3.IDENTITY.withTranslation(new Vec3(1.0D, 1.0D, 0.0D));
        Assertions.assertEquals(new Vec3(2.0D, 3.0D, 0.0D), evaluate(parent, local).translation());
    }

    @Test
    void pivotRotationUsesConfiguredPivot() {
        Transform3 transform = new Transform3(Vec3.ZERO, 90.0D, Vec3.ONE, new Vec3(1.0D, 0.0D, 0.0D));
        Assertions.assertEquals(new Vec3(1.0D, 1.0D, 0.0D), transform.applyPoint(new Vec3(2.0D, 0.0D, 0.0D)));
    }

    @Test
    void localToWorldTransformIsDeterministic() {
        Transform3 parent = Transform3.IDENTITY.withTranslation(new Vec3(2.0D, 0.0D, 0.0D));
        Transform3 local = Transform3.IDENTITY.withTranslation(new Vec3(1.0D, 0.0D, 0.0D));
        Assertions.assertEquals(evaluate(parent, local), evaluate(parent, local));
    }

    @Test
    void returnToBaseReturnsBase() {
        Assertions.assertEquals(0.0D, sampledY(AnimationEndBehavior.RETURN_TO_BASE, 2.0D,
                InterpolationKind.LINEAR), 0.0001D);
    }

    @Test
    void loopWrapsTime() {
        Assertions.assertEquals(0.5D, sampledY(AnimationEndBehavior.LOOP, 1.5D, InterpolationKind.LINEAR), 0.0001D);
    }

    @Test
    void stepInterpolationKeepsPreviousFrame() {
        Assertions.assertEquals(0.0D, sampledY(AnimationEndBehavior.HOLD, 0.5D, InterpolationKind.STEP), 0.0001D);
    }

    @Test
    void linearInterpolationInterpolatesMidpoint() {
        Assertions.assertEquals(0.5D, sampledY(AnimationEndBehavior.HOLD, 0.5D, InterpolationKind.LINEAR), 0.0001D);
    }

    @Test
    void durationValidationRejectsNonFinite() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ClipSampler().sample(List.of(track(InterpolationKind.LINEAR)), Double.NaN,
                        AnimationEndBehavior.HOLD, 0.0D));
    }

    @Test
    void duplicateKeyframeTimeRejected() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BoneTrack(root, List.of(
                frame(0.0D, 0.0D, InterpolationKind.LINEAR), frame(0.0D, 1.0D, InterpolationKind.LINEAR))));
    }

    @Test
    void eyeClosureZeroKeepsDefault() {
        Assertions.assertEquals(0.0D, controls.eyeClosureTravel(0.0D, 0.22D), 0.0001D);
    }

    @Test
    void eyeClosureOneReachesConfiguredTravel() {
        Assertions.assertEquals(0.22D, controls.eyeClosureTravel(1.0D, 0.22D), 0.0001D);
    }

    @Test
    void characterSpecificTravelDiffersAcrossRigs() {
        Assertions.assertNotEquals(controls.eyeClosureTravel(1.0D, 0.18D), controls.eyeClosureTravel(1.0D, 0.24D));
    }

    @Test
    void controlRejectsNonFiniteValue() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> controls.clampGaze(new Vec2(Double.NaN, 0.0D), Vec2.ZERO, new Vec2(1.0D, 1.0D)));
    }

    private Transform3 evaluate(Transform3 parent, Transform3 local) {
        var hierarchy = new BoneHierarchy(List.of(BoneDefinition.root(root), bone(child, root, local)));
        var pose = new BonePose(Map.of(root, parent));
        return new TransformEvaluator().evaluate(hierarchy, pose).get(child).orElseThrow();
    }

    private double sampledY(AnimationEndBehavior end, double time, InterpolationKind interpolation) {
        return new ClipSampler().sample(List.of(track(interpolation)), 1.0D, end, time)
                .transforms()
                .get(root)
                .translation()
                .y();
    }

    private BoneTrack track(InterpolationKind interpolation) {
        return new BoneTrack(root, List.of(frame(0.0D, 0.0D, interpolation), frame(1.0D, 1.0D, interpolation)));
    }

    private Keyframe frame(double time, double y, InterpolationKind interpolation) {
        return new Keyframe(time, Transform3.IDENTITY.withTranslation(new Vec3(0.0D, y, 0.0D)), interpolation);
    }

    private BoneDefinition bone(BoneId id, BoneId parent, Transform3 local) {
        return new BoneDefinition(id, Optional.of(parent), local);
    }
}
