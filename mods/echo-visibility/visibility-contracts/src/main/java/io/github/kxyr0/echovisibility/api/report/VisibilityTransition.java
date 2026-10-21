package io.github.kxyr0.echovisibility.api.report;

import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.api.state.VisibilityTransitionKind;
import java.util.Objects;

public record VisibilityTransition(
        VisibilityTransitionKind kind,
        VisibilityState previous,
        VisibilityState current,
        long previousStableFrame,
        long renderedFrame) {
    public VisibilityTransition {
        kind = Objects.requireNonNull(kind, "kind");
        previous = Objects.requireNonNull(previous, "previous");
        current = Objects.requireNonNull(current, "current");
        if (previousStableFrame < -1L || renderedFrame < 0L || previousStableFrame > renderedFrame) {
            throw new IllegalArgumentException("Visibility transition frame sequence is invalid.");
        }
    }
}
