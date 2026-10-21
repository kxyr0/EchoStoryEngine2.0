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

final class CoreEvaluationTest {
    private final BoneId root = BoneId.parse("face_root");
    private final BoneId child = BoneId.parse("upper_eyelids");

    @Test
    void duplicateBoneIdIsRejected() {
        var bones = List.of(BoneDefinition.root(root), BoneDefinition.root(root));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BoneHierarchy(bones));
    }

    @Test
    void missingParentIsRejected() {
        var bone = new BoneDefinition(child, Optional.of(root), Transform3.IDENTITY);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new BoneHierarchy(List.of(bone)));
    }

    @Test
    void parentTranslationAffectsChild() {
        Transform3 parent = Transform3.IDENTITY.withTranslation(new Vec3(2.0D, 0.0D, 0.0D));
        Transform3 local = Transform3.IDENTITY.withTranslation(new Vec3(1.0D, 0.0D, 0.0D));
        var hierarchy = new BoneHierarchy(List.of(new BoneDefinition(child, Optional.of(root), local),
                new BoneDefinition(root, Optional.empty(), parent)));
        var world = new TransformEvaluator().evaluate(hierarchy, new BonePose(Map.of()));
        Assertions.assertEquals(3.0D, world.get(child).orElseThrow().translation().x(), 0.0001D);
    }

    @Test
    void poseTranslationAddsToBindPose() {
        Transform3 local = Transform3.IDENTITY.withTranslation(new Vec3(1.0D, 0.0D, 0.0D));
        var hierarchy = new BoneHierarchy(List.of(BoneDefinition.root(root),
                new BoneDefinition(child, Optional.of(root), local)));
        var pose = new BonePose(Map.of(child, Transform3.IDENTITY.withTranslation(new Vec3(0.5D, 0.0D, 0.0D))));
        var world = new TransformEvaluator().evaluate(hierarchy, pose);
        Assertions.assertEquals(1.5D, world.get(child).orElseThrow().translation().x(), 0.0001D);
    }

    @Test
    void differentPivotProducesDifferentPoint() {
        Vec3 point = new Vec3(2.0D, 0.0D, 0.0D);
        Transform3 zeroPivot = new Transform3(Vec3.ZERO, 90.0D, Vec3.ONE, Vec3.ZERO);
        Transform3 localPivot = new Transform3(Vec3.ZERO, 90.0D, Vec3.ONE, new Vec3(1.0D, 0.0D, 0.0D));
        Assertions.assertNotEquals(zeroPivot.applyPoint(point), localPivot.applyPoint(point));
    }

    @Test
    void holdRetainsFinalSample() {
        BoneTrack track = new BoneTrack(root, List.of(
                new Keyframe(0.0D, Transform3.IDENTITY, InterpolationKind.LINEAR),
                new Keyframe(1.0D, Transform3.IDENTITY.withTranslation(new Vec3(0.0D, 1.0D, 0.0D)),
                        InterpolationKind.LINEAR)));
        var pose = new ClipSampler().sample(List.of(track), 1.0D, AnimationEndBehavior.HOLD, 2.0D);
        Assertions.assertEquals(1.0D, pose.transforms().get(root).translation().y(), 0.0001D);
    }

    @Test
    void gazeClampUsesConfiguredBounds() {
        var sampler = new FaceControlSampler();
        Vec2 gaze = sampler.clampGaze(new Vec2(5.0D, -5.0D), new Vec2(-1.0D, -2.0D), new Vec2(2.0D, 1.0D));
        Assertions.assertEquals(new Vec2(2.0D, -2.0D), gaze);
    }
}
