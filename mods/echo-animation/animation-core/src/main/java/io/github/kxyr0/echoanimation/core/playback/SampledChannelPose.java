package io.github.kxyr0.echoanimation.core.playback;

import io.github.kxyr0.echoanimation.api.graph.definition.AnimationChannelDefinition;
import java.util.Objects;

/**
 * One sampled visual channel with an explicit authored layer declaration.
 */
public record SampledChannelPose(AnimationChannelDefinition channel, AnimationPose pose, double weight) {
    public SampledChannelPose {
        channel = Objects.requireNonNull(channel, "channel");
        pose = Objects.requireNonNull(pose, "pose");
        if (!Double.isFinite(weight) || weight < 0.0D || weight > 1.0D) {
            throw new IllegalArgumentException("weight must be finite in range 0..1.");
        }
    }
}
