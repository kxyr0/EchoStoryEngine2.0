package io.github.kxyr0.echovisibility.api.report;

import io.github.kxyr0.echovisibility.api.identity.VisibilityConcealmentLeaseId;
import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.state.VisibilitySampleResult;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import java.util.Objects;
import java.util.Optional;

public record VisibilityReport(
        VisibilityWatchId watchId,
        VisibilityWatchGeneration generation,
        VisibilityDefinitionRevision targetRevision,
        long connectionGeneration,
        long reportSequence,
        long renderedFrame,
        long clientTick,
        VisibilityState state,
        VisibilityTransition transition,
        VisibilityCameraSnapshot camera,
        VisibilitySampleResult samples,
        Optional<VisibilityConcealmentLeaseId> concealmentLeaseId) {
    public VisibilityReport {
        watchId = Objects.requireNonNull(watchId, "watchId");
        generation = Objects.requireNonNull(generation, "generation");
        targetRevision = Objects.requireNonNull(targetRevision, "targetRevision");
        state = Objects.requireNonNull(state, "state");
        transition = Objects.requireNonNull(transition, "transition");
        camera = Objects.requireNonNull(camera, "camera");
        samples = Objects.requireNonNull(samples, "samples");
        concealmentLeaseId = Objects.requireNonNull(concealmentLeaseId, "concealmentLeaseId");
        if (connectionGeneration <= 0L || reportSequence <= 0L
                || renderedFrame < 0L || clientTick < 0L) {
            throw new IllegalArgumentException("Visibility report sequence values are invalid.");
        }
    }
}
