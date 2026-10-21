package io.github.kxyr0.echouianimations;

import java.util.Objects;

/**
 * Immutable animation preset. Channels are normalized values for renderer-owned UI state.
 */
public record AnimationPreset(
        String id,
        int durationMs,
        int delayMs,
        AnimationEasing easing,
        AnimationChannel alpha,
        AnimationChannel scale,
        AnimationChannel widthFactor,
        AnimationChannel heightFactor,
        boolean revealsText
) {
    public AnimationPreset {
        id = requireId(id);
        if (durationMs <= 0) {
            throw new IllegalArgumentException("durationMs must be positive.");
        }
        if (delayMs < 0) {
            throw new IllegalArgumentException("delayMs must not be negative.");
        }
        easing = Objects.requireNonNull(easing, "easing");
        alpha = Objects.requireNonNull(alpha, "alpha");
        scale = Objects.requireNonNull(scale, "scale");
        widthFactor = Objects.requireNonNull(widthFactor, "widthFactor");
        heightFactor = Objects.requireNonNull(heightFactor, "heightFactor");
    }

    private static String requireId(String value) {
        String trimmed = Objects.requireNonNull(value, "id").trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("id must not be blank.");
        }
        return trimmed;
    }
}
