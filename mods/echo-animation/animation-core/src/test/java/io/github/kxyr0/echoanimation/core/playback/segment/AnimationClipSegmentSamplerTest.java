package io.github.kxyr0.echoanimation.core.playback.segment;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipPoint;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationRootMotion;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipSegment;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.playback.NormalizedClipSampler;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class AnimationClipSegmentSamplerTest {
    private static final BoneId BODY = BoneId.parse("body");

    @Test
    void localSegmentProgressMapsIntoTheSharedSourceTimeline() {
        var pose = new NormalizedClipSampler().sample(clip(), new AnimationPose(java.util.Map.of()), 0.5D,
                Optional.of("run_loop"), Optional.empty());

        Assertions.assertEquals(8.0D, pose.transform(BODY).orElseThrow().translation().x(), 0.0001D);
    }

    @Test
    void namedPointSamplesOneExactSourceTick() {
        var pose = new NormalizedClipSampler().sample(clip(), new AnimationPose(java.util.Map.of()), 0.8D,
                Optional.empty(), Optional.of("neutral"));

        Assertions.assertEquals(2.0D, pose.transform(BODY).orElseThrow().translation().x(), 0.0001D);
    }

    private AnimationClipDefinition clip() {
        List<AnimationKeyframeDefinition> keys = List.of(frame(0), frame(2), frame(8), frame(14), frame(17));
        return new AnimationClipDefinition(AnimationClipId.parse("jdh:run"), Optional.empty(),
                List.of(new AnimationBoneTrackDefinition(BODY, keys)), 17,
                List.of(new AnimationClipSegment("idle_to_run", 0, 2),
                        new AnimationClipSegment("run_loop", 2, 14, AnimationLoopSeam.SMOOTH, 4.3D),
                        new AnimationClipSegment("run_to_idle", 14, 17)),
                List.of(new AnimationClipPoint("neutral", 2, Optional.of("run_loop"))), List.of(),
                AnimationRootMotion.disabled(), 2,
                AnimationLoopSeam.SMOOTH, 4.3D, "fingerprint");
    }

    private AnimationKeyframeDefinition frame(int tick) {
        return new AnimationKeyframeDefinition(tick / 17.0D, Optional.of(new Vec3(tick, 0.0D, 0.0D)),
                Optional.empty(), Optional.empty(), InterpolationKind.LINEAR);
    }
}
