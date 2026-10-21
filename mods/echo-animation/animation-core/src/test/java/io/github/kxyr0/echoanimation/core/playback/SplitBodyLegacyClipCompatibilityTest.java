package io.github.kxyr0.echoanimation.core.playback;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class SplitBodyLegacyClipCompatibilityTest {
    private static final BoneId BODY = BoneId.parse("body");
    private static final BoneId BODY_LOWER = BoneId.parse("body_lower");

    @Test
    void schemaTwoWholeBodyTrackMovesToLowerTorso() {
        AnimationPose sampled = new NormalizedClipSampler().sample(clip(2), new AnimationPose(Map.of()), 0.5D);

        Assertions.assertTrue(sampled.transform(BODY_LOWER).isPresent());
        Assertions.assertTrue(sampled.transform(BODY).isEmpty());
        Assertions.assertEquals(2.0D, sampled.transform(BODY_LOWER).orElseThrow().translation().y(), 0.0001D);
    }

    @Test
    void schemaThreeBodyTrackRemainsUpperTorso() {
        AnimationPose sampled = new NormalizedClipSampler().sample(clip(3), new AnimationPose(Map.of()), 0.5D);

        Assertions.assertTrue(sampled.transform(BODY).isPresent());
        Assertions.assertTrue(sampled.transform(BODY_LOWER).isEmpty());
        Assertions.assertEquals(2.0D, sampled.transform(BODY).orElseThrow().translation().y(), 0.0001D);
    }

    private AnimationClipDefinition clip(int schemaVersion) {
        AnimationBoneTrackDefinition track = new AnimationBoneTrackDefinition(BODY, List.of(
                frame(0.0D, 0.0D),
                frame(1.0D, 4.0D)
        ));
        return AnimationClipDefinition.legacy(AnimationClipId.parse("jdh:split_body_compat"),
                Optional.of(AnimationRigId.parse("jdh:humanoid")), List.of(track), new
                AnimationClipDefinition.LegacySettings(schemaVersion, AnimationLoopSeam.HOLD, 1.0D,
                "split-body-compat"));
    }

    private AnimationKeyframeDefinition frame(double progress, double y) {
        return new AnimationKeyframeDefinition(
                progress,
                Optional.of(new Vec3(0.0D, y, 0.0D)),
                Optional.empty(),
                Optional.empty(),
                InterpolationKind.LINEAR
        );
    }
}
