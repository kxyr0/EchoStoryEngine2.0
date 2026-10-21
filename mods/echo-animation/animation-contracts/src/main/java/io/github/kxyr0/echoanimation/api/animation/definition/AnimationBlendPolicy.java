package io.github.kxyr0.echoanimation.api.animation.definition;

import java.util.Objects;
import java.util.Optional;

/** Blend and mask policy used when an authored lifecycle edge is absent. */
public record AnimationBlendPolicy(
        int fallbackCrossfadeTicks,
        int blendInTicks,
        int blendOutTicks,
        Optional<String> maskId
) {
    public static final AnimationBlendPolicy NONE = new AnimationBlendPolicy(0, 0, 0, Optional.empty());

    public AnimationBlendPolicy {
        maskId = Objects.requireNonNull(maskId, "maskId");
        if (fallbackCrossfadeTicks < 0 || blendInTicks < 0 || blendOutTicks < 0) {
            throw new IllegalArgumentException("Animation blend ticks must not be negative.");
        }
        maskId = maskId.map(String::trim).filter(value -> !value.isEmpty());
    }

    public int enterTicks() {
        return blendInTicks > 0 ? blendInTicks : fallbackCrossfadeTicks;
    }

    public int exitTicks() {
        return blendOutTicks > 0 ? blendOutTicks : fallbackCrossfadeTicks;
    }
}
