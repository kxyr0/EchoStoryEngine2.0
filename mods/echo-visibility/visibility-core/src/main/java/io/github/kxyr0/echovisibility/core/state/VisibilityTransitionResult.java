package io.github.kxyr0.echovisibility.core.state;

import io.github.kxyr0.echovisibility.api.report.VisibilityTransition;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.api.state.VisibilityWatchStatus;
import java.util.Objects;

public record VisibilityTransitionResult(
        VisibilityState observedState,
        VisibilityState stableState,
        VisibilityTransition transition,
        VisibilityWatchStatus status,
        int hiddenFrames,
        int visibleFrames,
        boolean reportable) {
    public VisibilityTransitionResult {
        observedState = Objects.requireNonNull(observedState, "observedState");
        stableState = Objects.requireNonNull(stableState, "stableState");
        transition = Objects.requireNonNull(transition, "transition");
        status = Objects.requireNonNull(status, "status");
        if (hiddenFrames < 0 || visibleFrames < 0) {
            throw new IllegalArgumentException("Visibility frame counters must not be negative.");
        }
    }
}
