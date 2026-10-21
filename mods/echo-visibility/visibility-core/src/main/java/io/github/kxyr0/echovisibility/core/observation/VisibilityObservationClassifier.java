package io.github.kxyr0.echovisibility.core.observation;

import io.github.kxyr0.echovisibility.api.geometry.VisibilityAabb;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.api.policy.VisibilityDetectionMode;
import io.github.kxyr0.echovisibility.api.policy.VisibilityOcclusionMode;
import io.github.kxyr0.echovisibility.api.report.VisibilityCameraSnapshot;
import io.github.kxyr0.echovisibility.api.state.VisibilityFrustumClassification;
import io.github.kxyr0.echovisibility.api.state.VisibilitySampleResult;
import io.github.kxyr0.echovisibility.api.state.VisibilityState;
import io.github.kxyr0.echovisibility.core.math.VisibilityClipPoint;
import io.github.kxyr0.echovisibility.core.math.VisibilityFrustumClassifier;
import io.github.kxyr0.echovisibility.core.math.VisibilityFrustumResult;
import io.github.kxyr0.echovisibility.core.math.VisibilityProjection;
import io.github.kxyr0.echovisibility.core.occlusion.VisibilityOcclusionProbe;
import io.github.kxyr0.echovisibility.core.occlusion.VisibilityOcclusionResult;
import io.github.kxyr0.echovisibility.core.sample.VisibilitySampleGenerator;
import java.util.ArrayList;
import java.util.List;

public final class VisibilityObservationClassifier {
    private final VisibilityFrustumClassifier frustumClassifier = new VisibilityFrustumClassifier();
    private final VisibilitySampleGenerator sampleGenerator = new VisibilitySampleGenerator();
    private final VisibilityProjection projection = new VisibilityProjection();

    public VisibilityObservation evaluate(
            VisibilityWatchDefinition watch,
            VisibilityCameraSnapshot camera,
            VisibilityAabb bounds,
            VisibilityOcclusionProbe occlusionProbe) {
        VisibilityFrustumResult frustum = frustumClassifier.classify(camera, bounds, watch.edgeMarginPixels());
        VisibilityState broadState = broadState(frustum.classification());
        if (broadState != null) {
            return empty(broadState, frustum);
        }
        if (watch.occlusionMode() == VisibilityOcclusionMode.NONE
                || watch.detectionMode() == VisibilityDetectionMode.OUTSIDE_FRUSTUM) {
            VisibilityState state = frustum.classification() == VisibilityFrustumClassification.FULLY_INSIDE
                    ? VisibilityState.VISIBLE : VisibilityState.PARTIALLY_VISIBLE;
            return empty(state, frustum);
        }
        return evaluateSamples(watch, camera, bounds, occlusionProbe, frustum);
    }

    private VisibilityObservation evaluateSamples(
            VisibilityWatchDefinition watch,
            VisibilityCameraSnapshot camera,
            VisibilityAabb bounds,
            VisibilityOcclusionProbe probe,
            VisibilityFrustumResult frustum) {
        List<VisibilityEvaluatedSample> evaluated = new ArrayList<>();
        double marginX = 2.0D * watch.edgeMarginPixels() / camera.framebufferWidth();
        double marginY = 2.0D * watch.edgeMarginPixels() / camera.framebufferHeight();
        int viewport = 0;
        int visible = 0;
        int blocked = 0;
        int uncertain = 0;
        for (var sample : sampleGenerator.generate(bounds, watch.samplePolicy())) {
            VisibilityClipPoint clip = projection.project(camera, sample.position());
            boolean inViewport = clip.insideViewport(marginX, marginY);
            VisibilityOcclusionResult result = inViewport
                    ? safeProbe(probe, camera, sample.position())
                    : VisibilityOcclusionResult.uncertain();
            evaluated.add(new VisibilityEvaluatedSample(sample, clip, inViewport, result));
            if (inViewport) {
                viewport++;
                switch (result.status()) {
                    case CLEAR -> visible++;
                    case BLOCKED -> blocked++;
                    case UNCERTAIN -> uncertain++;
                }
            }
        }
        VisibilityState state = sampleState(frustum.classification(), viewport, visible, blocked, uncertain);
        String fingerprint = fingerprint(state, viewport, visible, blocked, uncertain);
        VisibilitySampleResult counts = new VisibilitySampleResult(evaluated.size(), viewport,
                visible, blocked, uncertain, fingerprint);
        return new VisibilityObservation(state, frustum, evaluated, counts);
    }

    private static VisibilityOcclusionResult safeProbe(
            VisibilityOcclusionProbe probe,
            VisibilityCameraSnapshot camera,
            io.github.kxyr0.echovisibility.api.geometry.VisibilityVector3 sample) {
        try {
            VisibilityOcclusionResult result = probe.probe(camera.position(), sample);
            return result == null ? VisibilityOcclusionResult.uncertain() : result;
        } catch (RuntimeException exception) {
            return VisibilityOcclusionResult.uncertain();
        }
    }

    private static VisibilityState sampleState(
            VisibilityFrustumClassification frustum,
            int viewport,
            int visible,
            int blocked,
            int uncertain) {
        if (visible > 0) {
            return frustum == VisibilityFrustumClassification.FULLY_INSIDE
                    && visible == viewport ? VisibilityState.VISIBLE : VisibilityState.PARTIALLY_VISIBLE;
        }
        if (viewport > 0 && blocked == viewport && uncertain == 0) {
            return VisibilityState.OCCLUDED;
        }
        return frustum == VisibilityFrustumClassification.FULLY_INSIDE
                ? VisibilityState.VISIBLE : VisibilityState.PARTIALLY_VISIBLE;
    }

    private static VisibilityState broadState(VisibilityFrustumClassification classification) {
        return switch (classification) {
            case FULLY_OUTSIDE -> VisibilityState.OUTSIDE_FRUSTUM;
            case BEHIND_CAMERA -> VisibilityState.BEHIND_CAMERA;
            case INVALID -> VisibilityState.CAMERA_UNAVAILABLE;
            case FULLY_INSIDE, INTERSECTING -> null;
        };
    }

    private static VisibilityObservation empty(VisibilityState state, VisibilityFrustumResult frustum) {
        return new VisibilityObservation(state, frustum, List.of(),
                new VisibilitySampleResult(0, 0, 0, 0, 0, fingerprint(state, 0, 0, 0, 0)));
    }

    private static String fingerprint(
            VisibilityState state,
            int viewport,
            int visible,
            int blocked,
            int uncertain) {
        String value = state + "|" + viewport + "|" + visible + "|" + blocked + "|" + uncertain;
        return Integer.toUnsignedString(value.hashCode(), 16);
    }
}
