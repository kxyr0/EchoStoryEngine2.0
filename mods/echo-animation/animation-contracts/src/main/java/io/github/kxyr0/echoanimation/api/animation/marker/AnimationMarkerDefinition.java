package io.github.kxyr0.echoanimation.api.animation.marker;

import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import java.util.Objects;

/**
 * One exactly-once marker position in a high-level animation lifecycle.
 */
public record AnimationMarkerDefinition(
        AnimationMarkerId id,
        AnimationPhase phase,
        long phaseTick
) {
    public AnimationMarkerDefinition {
        id = Objects.requireNonNull(id, "id");
        phase = Objects.requireNonNull(phase, "phase");
        if (phase == AnimationPhase.TERMINAL) {
            throw new IllegalArgumentException("Animation markers cannot target TERMINAL.");
        }
        if (phaseTick <= 0L) {
            throw new IllegalArgumentException("Animation marker phaseTick must be positive.");
        }
    }
}
