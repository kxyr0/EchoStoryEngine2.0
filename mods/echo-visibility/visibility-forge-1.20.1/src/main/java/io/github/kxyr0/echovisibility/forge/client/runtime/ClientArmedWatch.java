package io.github.kxyr0.echovisibility.forge.client.runtime;

import io.github.kxyr0.echovisibility.api.debug.VisibilityDebugSample;
import io.github.kxyr0.echovisibility.api.debug.VisibilityDebugSnapshot;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import io.github.kxyr0.echovisibility.api.state.VisibilityConcealmentStatus;
import io.github.kxyr0.echovisibility.api.state.VisibilityFrustumClassification;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.api.target.VisibilityTargetBounds;
import io.github.kxyr0.echovisibility.core.observation.VisibilityObservation;
import io.github.kxyr0.echovisibility.core.state.VisibilityTransitionResult;
import io.github.kxyr0.echovisibility.core.state.VisibilityTransitionState;
import java.util.List;
import java.util.Optional;

public final class ClientArmedWatch {
    private final VisibilityWatchDefinition definition;
    private final VisibilityWatchGeneration generation;
    private final long connectionGeneration;
    private final Optional<VisibilityTargetBounds> stableBounds;
    private final int clientEntityId;
    private final VisibilityTransitionState transitionState;
    private long reportSequence;
    private String acknowledgement = "NONE";
    private Optional<VisibilityDebugSnapshot> debug = Optional.empty();

    public ClientArmedWatch(
            VisibilityWatchDefinition definition,
            VisibilityWatchGeneration generation,
            long connectionGeneration,
            Optional<VisibilityTargetBounds> stableBounds,
            int clientEntityId) {
        this.definition = definition;
        this.generation = generation;
        this.connectionGeneration = connectionGeneration;
        this.stableBounds = stableBounds;
        this.clientEntityId = clientEntityId;
        this.transitionState = new VisibilityTransitionState(
                definition.hysteresis(), definition.requireVisibleFirst());
    }

    public long nextReportSequence() {
        return ++reportSequence;
    }

    public long reportSequence() {
        return reportSequence;
    }

    public void acknowledge(long sequence, boolean accepted, String message) {
        if (sequence == reportSequence) {
            acknowledgement = (accepted ? "ACCEPTED:" : "REJECTED:") + message;
        }
    }

    public void debug(
            Optional<VisibilityTargetBounds> bounds,
            VisibilityCameraSnapshot camera,
            VisibilityObservation observation,
            VisibilityTransitionResult transition,
            VisibilityConcealmentStatus concealment) {
        List<VisibilityDebugSample> samples = observation.samples().stream().map(value ->
                new VisibilityDebugSample(value.sample().kind(), value.sample().position(), value.inViewport(),
                        value.occlusion().status().name(), value.occlusion().hitPosition(),
                        value.occlusion().occluder())).toList();
        debug = Optional.of(new VisibilityDebugSnapshot(definition, generation, bounds, Optional.of(camera),
                transition.stableState(), observation.frustum().classification(),
                observation.frustum().projectedBounds(), samples, Optional.of(transition.transition()),
                transition.hiddenFrames(), transition.visibleFrames(), reportSequence,
                acknowledgement, concealment));
    }

    public void unavailable(VisibilityCameraSnapshot camera, VisibilityTransitionResult transition) {
        debug = Optional.of(new VisibilityDebugSnapshot(definition, generation, Optional.empty(),
                Optional.of(camera), transition.stableState(), VisibilityFrustumClassification.INVALID,
                Optional.empty(), List.of(), Optional.of(transition.transition()), transition.hiddenFrames(),
                transition.visibleFrames(), reportSequence, acknowledgement, VisibilityConcealmentStatus.NONE));
    }

    public VisibilityWatchDefinition definition() { return definition; }
    public VisibilityWatchGeneration generation() { return generation; }
    public long connectionGeneration() { return connectionGeneration; }
    public Optional<VisibilityTargetBounds> stableBounds() { return stableBounds; }
    public int clientEntityId() { return clientEntityId; }
    public VisibilityTransitionState transitionState() { return transitionState; }
    public Optional<VisibilityDebugSnapshot> debug() { return debug; }
}
