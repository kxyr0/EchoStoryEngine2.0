package io.github.kxyr0.echovisibility.api.policy;

public record VisibilityHysteresis(int lostFrames, int seenFrames) {
    public VisibilityHysteresis {
        if (lostFrames < 1 || lostFrames > 120 || seenFrames < 1 || seenFrames > 120) {
            throw new IllegalArgumentException("Visibility hysteresis must be within 1..120 rendered frames.");
        }
    }

    public static VisibilityHysteresis disappearanceDefaults() {
        return new VisibilityHysteresis(1, 2);
    }
}
