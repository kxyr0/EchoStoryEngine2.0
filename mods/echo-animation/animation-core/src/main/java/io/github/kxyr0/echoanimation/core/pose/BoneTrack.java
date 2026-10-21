package io.github.kxyr0.echoanimation.core.pose;

import io.github.kxyr0.echoanimation.api.id.BoneId;
import java.util.List;
import java.util.Objects;

/**
 * Ordered keyframes for one bone.
 */
public record BoneTrack(BoneId boneId, List<Keyframe> keyframes) {
    public BoneTrack {
        boneId = Objects.requireNonNull(boneId, "boneId");
        keyframes = List.copyOf(Objects.requireNonNull(keyframes, "keyframes"));
        requireOrdered(keyframes);
    }

    private static void requireOrdered(List<Keyframe> frames) {
        double previous = -1.0D;
        for (Keyframe frame : frames) {
            if (frame.timeSeconds() <= previous) {
                throw new IllegalArgumentException("Keyframe time должен строго возрастать.");
            }
            previous = frame.timeSeconds();
        }
    }
}

