package io.github.kxyr0.echoanimation.core.playback.rootmotion;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationRootMotion;
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

final class AnimationRootMotionSamplerTest {
    private static final BoneId ROOT = BoneId.parse("root");

    @Test
    void extractedRootIsSeparateFromLocalPose() {
        AnimationClipDefinition clip = clip(AnimationRootMotion.Mode.EXTRACT);
        NormalizedClipSampler sampler = new NormalizedClipSampler();

        Assertions.assertTrue(sampler.sample(clip, AnimationPose.EMPTY, 1.0D).transform(ROOT).isEmpty());
        Assertions.assertEquals(new Vec3(16.0D, 0.0D, 0.0D), sampler.sampleRootMotion(clip, 1.0D)
                .orElseThrow().translation());
    }

    @Test
    void preservedRootRemainsInLocalPose() {
        AnimationClipDefinition clip = clip(AnimationRootMotion.Mode.PRESERVE);
        NormalizedClipSampler sampler = new NormalizedClipSampler();

        Assertions.assertTrue(sampler.sample(clip, AnimationPose.EMPTY, 1.0D).transform(ROOT).isPresent());
        Assertions.assertTrue(sampler.sampleRootMotion(clip, 1.0D).isEmpty());
    }

    private AnimationClipDefinition clip(AnimationRootMotion.Mode mode) {
        var keys = List.of(frame(0.0D, 0.0D), frame(1.0D, 16.0D));
        return new AnimationClipDefinition(AnimationClipId.parse("jdh:root_motion"), Optional.empty(),
                List.of(new AnimationBoneTrackDefinition(ROOT, keys)), 20, List.of(), List.of(), List.of(),
                new AnimationRootMotion(mode, ROOT), 3, AnimationLoopSeam.HOLD, 1.0D, "fingerprint");
    }

    private AnimationKeyframeDefinition frame(double progress, double x) {
        return new AnimationKeyframeDefinition(progress, Optional.of(new Vec3(x, 0.0D, 0.0D)), Optional.empty(),
                Optional.empty(), InterpolationKind.LINEAR);
    }
}
