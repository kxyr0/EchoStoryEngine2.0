package io.github.kxyr0.echovisibility.api.debug;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityScreenRectangle;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchGeneration;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import io.github.kxyr0.echovisibility.api.report.VisibilityTransition;
import io.github.kxyr0.echovisibility.api.state.VisibilityConcealmentStatus;
import io.github.kxyr0.echovisibility.api.state.VisibilityFrustumClassification;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.api.target.VisibilityTargetBounds;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record VisibilityDebugSnapshot(
        VisibilityWatchDefinition definition,
        VisibilityWatchGeneration generation,
        Optional<VisibilityTargetBounds> targetBounds,
        Optional<VisibilityCameraSnapshot> camera,
        VisibilityState state,
        VisibilityFrustumClassification frustum,
        Optional<VisibilityScreenRectangle> projectedBounds,
        List<VisibilityDebugSample> samples,
        Optional<VisibilityTransition> transition,
        int hiddenFrames,
        int visibleFrames,
        long reportSequence,
        String serverAcknowledgement,
        VisibilityConcealmentStatus concealmentStatus) {
    public VisibilityDebugSnapshot {
        definition = Objects.requireNonNull(definition, "definition");
        generation = Objects.requireNonNull(generation, "generation");
        targetBounds = Objects.requireNonNull(targetBounds, "targetBounds");
        camera = Objects.requireNonNull(camera, "camera");
        state = Objects.requireNonNull(state, "state");
        frustum = Objects.requireNonNull(frustum, "frustum");
        projectedBounds = Objects.requireNonNull(projectedBounds, "projectedBounds");
        samples = List.copyOf(Objects.requireNonNull(samples, "samples"));
        transition = Objects.requireNonNull(transition, "transition");
        serverAcknowledgement = Objects.requireNonNull(serverAcknowledgement, "serverAcknowledgement");
    }
}
