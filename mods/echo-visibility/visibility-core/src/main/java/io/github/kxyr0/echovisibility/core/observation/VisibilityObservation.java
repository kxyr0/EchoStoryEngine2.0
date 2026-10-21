package io.github.kxyr0.echovisibility.core.observation;

import io.github.kxyr0.echovisibility.api.state.VisibilitySampleResult;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.core.math.VisibilityFrustumResult;
import java.util.List;
import java.util.Objects;

public record VisibilityObservation(
        VisibilityState state,
        VisibilityFrustumResult frustum,
        List<VisibilityEvaluatedSample> samples,
        VisibilitySampleResult sampleResult) {
    public VisibilityObservation {
        state = Objects.requireNonNull(state, "state");
        frustum = Objects.requireNonNull(frustum, "frustum");
        samples = List.copyOf(Objects.requireNonNull(samples, "samples"));
        sampleResult = Objects.requireNonNull(sampleResult, "sampleResult");
    }
}
