package io.github.kxyr0.echoanimation.core.sync;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.ChannelCompositionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigKind;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationSkinVariant;
import io.github.kxyr0.echoanimation.api.rig.definition.SegmentedHumanoidBone;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.api.sync.AnimationChannelSyncState;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.api.sync.AnimationSyncRevision;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationClientPoseResolverTest {
    private static final AnimationRigId RIG_ID = AnimationRigId.parse("jdh:humanoid");
    private static final BoneId RIGHT_FOREARM = SegmentedHumanoidBone.RIGHT_FOREARM.id();
    private static final BoneId LEFT_FOREARM = SegmentedHumanoidBone.LEFT_FOREARM.id();

    @Test
    void resolvesMaskedChannelFromAuthoritativeSnapshot() {
        AnimationSubjectSyncSnapshot snapshot = snapshot(channel(AnimationClipId.parse("jdh:right"),
                Set.of(RIGHT_FOREARM), Optional.empty(), 1.0D));

        var pose = new AnimationClientPoseResolver().resolve(rig(), snapshot,
                List.of(clip("jdh:right", RIGHT_FOREARM, 20.0D), clip("jdh:left", LEFT_FOREARM, 50.0D)));

        Assertions.assertEquals(20.0D, pose.transform(RIGHT_FOREARM).orElseThrow().rotationDegrees().z(), 0.0001D);
        Assertions.assertEquals(0.0D, pose.transform(LEFT_FOREARM).orElseThrow().rotationDegrees().z(), 0.0001D);
    }

    @Test
    void blendsPreviousAndCurrentClipDuringTransition() {
        AnimationSubjectSyncSnapshot snapshot = snapshot(channel(AnimationClipId.parse("jdh:wave"),
                Set.of(RIGHT_FOREARM), Optional.of(AnimationClipId.parse("jdh:idle")), 0.5D));

        var pose = new AnimationClientPoseResolver().resolve(rig(), snapshot,
                List.of(clip("jdh:idle", RIGHT_FOREARM, 0.0D), clip("jdh:wave", RIGHT_FOREARM, 90.0D)));

        Assertions.assertEquals(45.0D, pose.transform(RIGHT_FOREARM).orElseThrow().rotationDegrees().z(), 0.0001D);
    }

    @Test
    void advancesPlaybackFromAuthoritativeTickWithoutAnotherPacket() {
        AnimationSubjectSyncSnapshot snapshot = new AnimationSubjectSyncSnapshot(new AnimationSubjectId("jdh:actor"),
                1L, 100L, new AnimationSyncRevision(1L), List.of(loopChannel()));

        var pose = new AnimationClientPoseResolver().resolve(rig(), snapshot,
                List.of(animatedClip("jdh:wave", RIGHT_FOREARM, 100.0D)), 105L);

        Assertions.assertEquals(50.0D, pose.transform(RIGHT_FOREARM).orElseThrow().rotationDegrees().z(), 0.0001D);
    }

    @Test
    void advancesPlaybackWithFractionalRenderTick() {
        AnimationSubjectSyncSnapshot snapshot = new AnimationSubjectSyncSnapshot(new AnimationSubjectId("jdh:actor"),
                1L, 100L, new AnimationSyncRevision(1L), List.of(loopChannel()));

        var pose = new AnimationClientPoseResolver().resolve(rig(), snapshot,
                List.of(animatedClip("jdh:wave", RIGHT_FOREARM, 100.0D)), 105.5D);

        Assertions.assertEquals(55.0D, pose.transform(RIGHT_FOREARM).orElseThrow().rotationDegrees().z(), 0.0001D);
    }

    @Test
    void anchorsPlaybackSpeedChangesToAuthoritativeProgress() {
        AnimationChannelSyncState channel = new AnimationChannelSyncState(new AnimationChannelId("body"),
                new AnimationStateId("body/wave"), Optional.of(AnimationClipId.parse("jdh:wave")),
                Optional.empty(), Optional.empty(),
                4L, 40L, 100L, 0.4D, 0L, false, false, PlaybackMode.LOOP, 1.75D, 0,
                ChannelCompositionPolicy.OVERRIDE, Set.of(RIGHT_FOREARM), 1.0D, Optional.empty(), 0L, 0L, 1.0D);
        AnimationSubjectSyncSnapshot snapshot = new AnimationSubjectSyncSnapshot(new AnimationSubjectId("jdh:actor"),
                1L, 100L, new AnimationSyncRevision(1L), List.of(channel));

        var pose = new AnimationClientPoseResolver().resolve(rig(), snapshot,
                List.of(animatedClip("jdh:wave", RIGHT_FOREARM, 100.0D)), 100L);

        Assertions.assertEquals(40.0D, pose.transform(RIGHT_FOREARM).orElseThrow().rotationDegrees().z(), 0.0001D);
    }

    @Test
    void advancesPlaybackSpeedChangesFromAuthoritativeProgress() {
        AnimationChannelSyncState channel = new AnimationChannelSyncState(new AnimationChannelId("body"),
                new AnimationStateId("body/wave"), Optional.of(AnimationClipId.parse("jdh:wave")),
                Optional.empty(), Optional.empty(),
                4L, 40L, 100L, 0.4D, 0L, false, false, PlaybackMode.LOOP, 2.0D, 0,
                ChannelCompositionPolicy.OVERRIDE, Set.of(RIGHT_FOREARM), 1.0D, Optional.empty(), 0L, 0L, 1.0D);
        AnimationSubjectSyncSnapshot snapshot = new AnimationSubjectSyncSnapshot(new AnimationSubjectId("jdh:actor"),
                1L, 100L, new AnimationSyncRevision(1L), List.of(channel));

        var pose = new AnimationClientPoseResolver().resolve(rig(), snapshot,
                List.of(animatedClip("jdh:wave", RIGHT_FOREARM, 100.0D)), 105L);

        Assertions.assertEquals(50.0D, pose.transform(RIGHT_FOREARM).orElseThrow().rotationDegrees().z(), 0.0001D);
    }

    @Test
    void advancesTransitionBlendFromAuthoritativeTickWithoutAnotherPacket() {
        AnimationSubjectSyncSnapshot snapshot = new AnimationSubjectSyncSnapshot(new AnimationSubjectId("jdh:actor"),
                1L, 100L, new AnimationSyncRevision(1L), List.of(channel(AnimationClipId.parse("jdh:wave"),
                        Set.of(RIGHT_FOREARM), Optional.of(AnimationClipId.parse("jdh:idle")), 0.0D)));

        var pose = new AnimationClientPoseResolver().resolve(rig(), snapshot,
                List.of(clip("jdh:idle", RIGHT_FOREARM, 0.0D), clip("jdh:wave", RIGHT_FOREARM, 90.0D)),
                105L);

        Assertions.assertEquals(45.0D, pose.transform(RIGHT_FOREARM).orElseThrow().rotationDegrees().z(), 0.0001D);
    }

    private AnimationSubjectSyncSnapshot snapshot(AnimationChannelSyncState channel) {
        return new AnimationSubjectSyncSnapshot(new AnimationSubjectId("jdh:actor"), 1L,
                new AnimationSyncRevision(1L), List.of(channel));
    }

    private AnimationChannelSyncState channel(
            AnimationClipId clipId,
            Set<BoneId> mask,
            Optional<AnimationClipId> previousClipId,
            double transitionProgress
    ) {
        return new AnimationChannelSyncState(new AnimationChannelId("body"), new AnimationStateId("body/test"),
                Optional.of(clipId), Optional.empty(), Optional.empty(), 0L, 0L, 20L, 0.0D, 0L, false, false,
                PlaybackMode.HOLD_LAST_FRAME, 1.0D, 10, ChannelCompositionPolicy.OVERRIDE, mask, 1.0D,
                previousClipId, (long) (transitionProgress * 10.0D),
                previousClipId.isPresent() ? 10L : 0L, transitionProgress);
    }

    private AnimationChannelSyncState loopChannel() {
        return new AnimationChannelSyncState(new AnimationChannelId("body"), new AnimationStateId("body/wave"),
                Optional.of(AnimationClipId.parse("jdh:wave")), Optional.empty(), Optional.empty(),
                0L, 0L, 10L, 0.0D, 0L, false, false,
                PlaybackMode.LOOP, 1.0D, 0, ChannelCompositionPolicy.OVERRIDE, Set.of(RIGHT_FOREARM), 1.0D,
                Optional.empty(), 0L, 0L, 1.0D);
    }

    private AnimationRigDefinition rig() {
        return new AnimationRigDefinition(RIG_ID, 1L, AnimationRigKind.SEGMENTED_HUMANOID,
                AnimationSkinVariant.CLASSIC, SegmentedHumanoidBone.defaultDefinitions(), List.of(), List.of(),
                "fingerprint");
    }

    private AnimationClipDefinition clip(String id, BoneId boneId, double roll) {
        return new AnimationClipDefinition(AnimationClipId.parse(id), Optional.of(RIG_ID),
                List.of(new AnimationBoneTrackDefinition(boneId, List.of(
                        new AnimationKeyframeDefinition(0.0D, Optional.empty(),
                                Optional.of(new Vec3(0.0D, 0.0D, roll)), Optional.empty(),
                                InterpolationKind.LINEAR)))),
                "fingerprint");
    }

    private AnimationClipDefinition animatedClip(String id, BoneId boneId, double roll) {
        return new AnimationClipDefinition(AnimationClipId.parse(id), Optional.of(RIG_ID),
                List.of(new AnimationBoneTrackDefinition(boneId, List.of(
                        new AnimationKeyframeDefinition(0.0D, Optional.empty(),
                                Optional.of(Vec3.ZERO), Optional.empty(), InterpolationKind.LINEAR),
                        new AnimationKeyframeDefinition(1.0D, Optional.empty(),
                                Optional.of(new Vec3(0.0D, 0.0D, roll)), Optional.empty(),
                                InterpolationKind.LINEAR)))),
                "fingerprint");
    }
}
