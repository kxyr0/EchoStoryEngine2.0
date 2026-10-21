package io.github.kxyr0.echoanimation.core;

import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationGraphId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationChannelDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationGraphDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationStateDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.ChannelCompositionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.InterruptionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationAttachmentSocketDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigKind;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.api.rig.definition.SegmentedHumanoidBone;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.playback.ChannelPoseComposition;
import io.github.kxyr0.echoanimation.core.playback.PoseBlender;
import io.github.kxyr0.echoanimation.core.playback.SampledChannelPose;
import io.github.kxyr0.echoanimation.core.rig.RigPoseEvaluator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class A2RigCompositionTest {
    private static final BoneId RIGHT_FOREARM = SegmentedHumanoidBone.RIGHT_FOREARM.id();
    private static final BoneId RIGHT_SHOULDER = SegmentedHumanoidBone.RIGHT_SHOULDER.id();
    private static final BoneId RIGHT_THIGH = SegmentedHumanoidBone.RIGHT_THIGH.id();
    private static final BoneId RIGHT_SHIN = SegmentedHumanoidBone.RIGHT_SHIN.id();

    @Test
    void shoulderAndKneeParentTransformsAffectChildren() {
        AnimationPose pose = new AnimationPose(Map.of(
                RIGHT_SHOULDER, defaultWithRoll(SegmentedHumanoidBone.RIGHT_SHOULDER, 90.0D),
                RIGHT_THIGH, defaultWithRoll(SegmentedHumanoidBone.RIGHT_THIGH, 90.0D)));
        var evaluated = new RigPoseEvaluator().evaluate(rig(), pose);

        Assertions.assertEquals(2.0D, evaluated.bone(RIGHT_FOREARM).orElseThrow().translation().x(), 0.0001D);
        Assertions.assertEquals(22.0D, evaluated.bone(RIGHT_FOREARM).orElseThrow().translation().y(), 0.0001D);
        Assertions.assertEquals(4.0D, evaluated.bone(RIGHT_SHIN).orElseThrow().translation().x(), 0.0001D);
    }

    @Test
    void socketInheritsAnimatedForearmTransform() {
        AnimationPose pose = new AnimationPose(Map.of(
                RIGHT_SHOULDER, defaultWithRoll(SegmentedHumanoidBone.RIGHT_SHOULDER, 90.0D)));
        var socket = new RigPoseEvaluator().evaluate(rig(), pose)
                .socket(AnimationAttachmentSocketId.parse("main_hand"))
                .orElseThrow();
        Assertions.assertEquals(8.0D, socket.translation().x(), 0.0001D);
        Assertions.assertEquals(22.0D, socket.translation().y(), 0.0001D);
    }

    @Test
    void overrideOrderingAndAdditiveCompositionAreDeterministic() {
        AnimationChannelDefinition base = channel("base", 0, ChannelCompositionPolicy.OVERRIDE);
        AnimationChannelDefinition upper = channel("upper_body", 10, ChannelCompositionPolicy.OVERRIDE);
        AnimationChannelDefinition add = channel("additive", 20, ChannelCompositionPolicy.ADDITIVE);
        var composed = new ChannelPoseComposition().compose(AnimationPose.EMPTY, List.of(
                sampled(upper, 2.0D, 0.0D),
                sampled(add, 0.0D, 3.0D),
                sampled(base, 1.0D, 0.0D)));

        EulerTransform result = composed.transform(RIGHT_FOREARM).orElseThrow();
        Assertions.assertEquals(2.0D, result.translation().x(), 0.0001D);
        Assertions.assertEquals(3.0D, result.translation().y(), 0.0001D);
    }

    @Test
    void overlappingOverrideChannelsWithSameOrderAreRejected() {
        AnimationChannelDefinition first = channel("first", 0, ChannelCompositionPolicy.OVERRIDE);
        AnimationChannelDefinition second = channel("second", 0, ChannelCompositionPolicy.OVERRIDE);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AnimationGraphDefinition(
                new AnimationGraphId("jdh:bad"), 1L, List.of(first, second),
                List.of(state(first.initialStateId(), first.id()), state(second.initialStateId(), second.id())),
                List.of()));
    }

    @Test
    void transitionBlendUsesShortestRotationPath() {
        EulerTransform left = new EulerTransform(Vec3.ZERO, new Vec3(0.0D, 0.0D, 350.0D), Vec3.ONE, Vec3.ZERO);
        EulerTransform right = new EulerTransform(Vec3.ZERO, new Vec3(0.0D, 0.0D, 10.0D), Vec3.ONE, Vec3.ZERO);
        Assertions.assertEquals(360.0D, new PoseBlender().blend(left, right, 0.5D)
                .rotationDegrees().z(), 0.0001D);
    }

    private SampledChannelPose sampled(AnimationChannelDefinition channel, double x, double y) {
        return new SampledChannelPose(channel,
                new AnimationPose(Map.of(RIGHT_FOREARM, moved(x, y))), 1.0D);
    }

    private AnimationChannelDefinition channel(String id, int order, ChannelCompositionPolicy policy) {
        return new AnimationChannelDefinition(new AnimationChannelId(id),
                new AnimationStateId(id + "/idle"), order, policy, Set.of(RIGHT_FOREARM));
    }

    private AnimationStateDefinition state(AnimationStateId id, AnimationChannelId channelId) {
        return new AnimationStateDefinition(id, channelId, 5L,
                PlaybackMode.HOLD_LAST_FRAME, InterruptionPolicy.IMMEDIATE, Optional.empty(), 1.0D);
    }

    private AnimationRigDefinition rig() {
        return new AnimationRigDefinition(AnimationRigId.parse("jdh:humanoid"), 1L,
                AnimationRigKind.SEGMENTED_HUMANOID, AnimationSkinVariant.CLASSIC,
                SegmentedHumanoidBone.defaultDefinitions(), List.of(), sockets(), "fingerprint");
    }

    private List<AnimationAttachmentSocketDefinition> sockets() {
        return List.of(new AnimationAttachmentSocketDefinition(AnimationAttachmentSocketId.parse("main_hand"),
                RIGHT_FOREARM, moved(0.0D, -6.0D)));
    }

    private EulerTransform defaultWithRoll(SegmentedHumanoidBone bone, double degrees) {
        EulerTransform defaults = SegmentedHumanoidBone.defaultDefinitions().stream()
                .filter(definition -> definition.id().equals(bone.id()))
                .findFirst()
                .orElseThrow()
                .defaultLocalTransform();
        return new EulerTransform(defaults.translation(), new Vec3(0.0D, 0.0D, degrees), Vec3.ONE, Vec3.ZERO);
    }

    private EulerTransform moved(double x, double y) {
        return EulerTransform.IDENTITY.withTranslation(new Vec3(x, y, 0.0D));
    }
}
