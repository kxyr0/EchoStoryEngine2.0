package io.github.kxyr0.echoanimation.core.playback;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.Matrix4Transform;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.api.model.InterpolationKind;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class MatrixClipSamplingTest {
    private static final BoneId ROOT = BoneId.parse("root");

    @Test
    void matrixTrackInterpolatesWithoutDecomposingShear() {
        Matrix4Transform left = matrix(0.0D, 0.25D);
        Matrix4Transform right = matrix(10.0D, 0.5D);
        var track = new AnimationBoneTrackDefinition(ROOT, List.of(
                frame(0.0D, left),
                frame(1.0D, right)));
        AnimationClipDefinition clip = AnimationClipDefinition.legacy(AnimationClipId.parse("test:matrix"),
                Optional.empty(), List.of(track), new AnimationClipDefinition.LegacySettings(2,
                AnimationLoopSeam.HOLD, 1.0D, "matrix"));

        Matrix4Transform sampled = new NormalizedClipSampler()
                .sample(clip, AnimationPose.EMPTY, 0.5D)
                .matrixTransform(ROOT)
                .orElseThrow();

        Assertions.assertEquals(5.0D, sampled.value(0, 3), 0.000001D);
        Assertions.assertEquals(0.375D, sampled.value(0, 1), 0.000001D);
    }

    @Test
    void trackRejectsMixedMatrixAndTrsKeys() {
        Assertions.assertThrows(IllegalArgumentException.class, () ->
                new AnimationBoneTrackDefinition(ROOT, List.of(
                        frame(0.0D, matrix(0.0D, 0.0D)),
                        new AnimationKeyframeDefinition(1.0D, Optional.empty(), Optional.empty(),
                                Optional.empty(), InterpolationKind.LINEAR))));
    }

    private AnimationKeyframeDefinition frame(double progress, Matrix4Transform matrix) {
        return new AnimationKeyframeDefinition(progress, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(matrix), InterpolationKind.LINEAR);
    }

    private Matrix4Transform matrix(double x, double shear) {
        return Matrix4Transform.fromRowMajor(
                1.0D, shear, 0.0D, x,
                0.0D, 1.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 1.0D, 0.0D,
                0.0D, 0.0D, 0.0D, 1.0D);
    }
}
