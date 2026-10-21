package io.github.kxyr0.echoanimation.api.clip.definition;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import java.util.List;
import java.util.Objects;

/**
 * Ordered normalized keyframes for one bone.
 */
public record AnimationBoneTrackDefinition(BoneId boneId, List<AnimationKeyframeDefinition> keyframes) {
    public AnimationBoneTrackDefinition {
        boneId = Objects.requireNonNull(boneId, "boneId");
        keyframes = List.copyOf(Objects.requireNonNull(keyframes, "keyframes"));
        requireOrdered(keyframes);
        requireConsistentEncoding(keyframes);
    }

    public boolean matrixTrack() {
        return !keyframes.isEmpty() && keyframes.get(0).localMatrix().isPresent();
    }

    private static void requireConsistentEncoding(List<AnimationKeyframeDefinition> frames) {
        Boolean matrix = null;
        for (AnimationKeyframeDefinition frame : frames) {
            boolean current = frame.localMatrix().isPresent();
            if (matrix == null) {
                matrix = current;
            } else if (matrix != current) {
                throw new IllegalArgumentException("A bone track cannot mix matrix and TRS keyframes.");
            }
        }
    }

    private static void requireOrdered(List<AnimationKeyframeDefinition> frames) {
        double previous = -1.0D;
        for (AnimationKeyframeDefinition frame : frames) {
            if (frame.progress() <= previous) {
                throw new IllegalArgumentException("Keyframe progress must strictly increase.");
            }
            previous = frame.progress();
        }
    }
}
