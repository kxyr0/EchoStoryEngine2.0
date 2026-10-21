package io.github.kxyr0.echoanimation.core;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationStateDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.InterruptionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.EulerTransform;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.playback.NormalizedClipSampler;
import io.github.kxyr0.echoanimation.core.playback.PlaybackCursor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class A2ClipPlaybackTest {
    private static final BoneId ROOT = BoneId.parse("root");
    private static final BoneId ARM = BoneId.parse("right_forearm");

    @Test
    void normalizedClipSamplingCoversLinearRotationScaleAndFallback() {
        AnimationClipDefinition clip = clip(InterpolationKind.LINEAR);
        AnimationPose base = new AnimationPose(Map.of(ROOT, moved(3.0D), ARM, EulerTransform.IDENTITY));
        AnimationPose pose = new NormalizedClipSampler().sample(clip, base, 0.25D);

        EulerTransform arm = pose.transform(ARM).orElseThrow();
        Assertions.assertEquals(5.0D, arm.translation().y(), 0.0001D);
        Assertions.assertEquals(360.0D, arm.rotationDegrees().z(), 0.0001D);
        Assertions.assertEquals(1.5D, arm.scale().x(), 0.0001D);
        Assertions.assertEquals(3.0D, pose.transform(ROOT).orElseThrow().translation().x(), 0.0001D);
    }

    @Test
    void stepSamplingKeepsPreviousFrame() {
        AnimationPose pose = new NormalizedClipSampler().sample(clip(InterpolationKind.STEP),
                new AnimationPose(Map.of(ARM, EulerTransform.IDENTITY)), 0.25D);
        Assertions.assertEquals(0.0D, pose.transform(ARM).orElseThrow().translation().y(), 0.0001D);
    }

    @Test
    void smoothSamplingEasesWithinSegment() {
        AnimationPose pose = new NormalizedClipSampler().sample(clip(InterpolationKind.SMOOTH),
                new AnimationPose(Map.of(ARM, EulerTransform.IDENTITY)), 0.125D);

        EulerTransform arm = pose.transform(ARM).orElseThrow();
        Assertions.assertEquals(1.5625D, arm.translation().y(), 0.0001D);
        Assertions.assertEquals(353.125D, arm.rotationDegrees().z(), 0.0001D);
    }

    @Test
    void cubicSamplingUsesAutoHermiteTangents() {
        AnimationClipDefinition clip = new AnimationClipDefinition(AnimationClipId.parse("jdh:cubic"),
                Optional.empty(), List.of(new AnimationBoneTrackDefinition(ARM, List.of(
                        frame(0.0D, 0.0D, 350.0D, 1.0D, InterpolationKind.CUBIC),
                        frame(0.5D, 10.0D, 10.0D, 2.0D, InterpolationKind.LINEAR),
                        frame(1.0D, 0.0D, 350.0D, 1.0D, InterpolationKind.LINEAR)))),
                "fingerprint");

        AnimationPose pose = new NormalizedClipSampler().sample(clip,
                new AnimationPose(Map.of(ARM, EulerTransform.IDENTITY)), 0.25D);

        EulerTransform arm = pose.transform(ARM).orElseThrow();
        Assertions.assertEquals(6.25D, arm.translation().y(), 0.0001D);
        Assertions.assertTrue(Double.isFinite(arm.rotationDegrees().z()));
    }

    @Test
    void loopSeamCanInterpolateImplicitTailToFirstFrame() {
        AnimationClipDefinition clip = AnimationClipDefinition.legacy(AnimationClipId.parse("jdh:loop_tail"),
                Optional.empty(), List.of(new AnimationBoneTrackDefinition(ARM, List.of(
                        frame(0.0D, 0.0D, 0.0D, 1.0D, InterpolationKind.LINEAR),
                        frame(0.5D, 10.0D, 0.0D, 1.0D, InterpolationKind.LINEAR)))), new
                AnimationClipDefinition.LegacySettings(2, AnimationLoopSeam.LINEAR, 1.2D, "fingerprint"));

        AnimationPose pose = new NormalizedClipSampler().sample(clip,
                new AnimationPose(Map.of(ARM, EulerTransform.IDENTITY)), 0.75D);

        Assertions.assertEquals(5.0D, pose.transform(ARM).orElseThrow().translation().y(), 0.0001D);
    }

    @Test
    void playbackCursorUsesServerTicksOnly() {
        PlaybackCursor loop = PlaybackCursor.from(state(PlaybackMode.LOOP, InterruptionPolicy.IMMEDIATE), 7L);
        PlaybackCursor hold = PlaybackCursor.from(state(PlaybackMode.HOLD_LAST_FRAME,
                InterruptionPolicy.WAIT_FOR_EXIT), 99L);

        Assertions.assertEquals(2L, loop.sampledTick());
        Assertions.assertEquals(1L, loop.loopIteration());
        Assertions.assertFalse(loop.clipComplete());
        Assertions.assertEquals(5L, hold.sampledTick());
        Assertions.assertTrue(hold.clipComplete());
        Assertions.assertTrue(hold.waitingForExit());
    }

    private AnimationStateDefinition state(PlaybackMode mode, InterruptionPolicy interruption) {
        return new AnimationStateDefinition(new AnimationStateId("body/wave"), new AnimationChannelId("body"),
                5L, mode, interruption, Optional.of(AnimationClipId.parse("jdh:humanoid_test_wave")), 1.0D);
    }

    private AnimationClipDefinition clip(InterpolationKind interpolation) {
        return new AnimationClipDefinition(AnimationClipId.parse("jdh:humanoid_test_wave"), Optional.empty(),
                List.of(new AnimationBoneTrackDefinition(ARM, List.of(
                        frame(0.0D, 0.0D, 350.0D, 1.0D, interpolation),
                        frame(0.5D, 10.0D, 10.0D, 2.0D, InterpolationKind.LINEAR)))),
                "fingerprint");
    }

    private AnimationKeyframeDefinition frame(
            double progress,
            double y,
            double roll,
            double scale,
            InterpolationKind interpolation
    ) {
        return new AnimationKeyframeDefinition(progress, Optional.of(new Vec3(0.0D, y, 0.0D)),
                Optional.of(new Vec3(0.0D, 0.0D, roll)), Optional.of(new Vec3(scale, scale, scale)), interpolation);
    }

    private EulerTransform moved(double x) {
        return EulerTransform.IDENTITY.withTranslation(new Vec3(x, 0.0D, 0.0D));
    }
}
