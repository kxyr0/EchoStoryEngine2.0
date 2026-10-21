package io.github.kxyr0.echovisibility.api.model;

import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.policy.VisibilityConcealmentPolicy;
import io.github.kxyr0.echovisibility.api.policy.VisibilityDetectionMode;
import io.github.kxyr0.echovisibility.api.policy.VisibilityEvaluationRate;
import io.github.kxyr0.echovisibility.api.policy.VisibilityHysteresis;
import io.github.kxyr0.echovisibility.api.policy.VisibilityOcclusionMode;
import io.github.kxyr0.echovisibility.api.policy.VisibilityViewpoint;
import io.github.kxyr0.echovisibility.api.sample.VisibilitySamplePolicy;
import io.github.kxyr0.echovisibility.api.target.VisibilityBoxTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityNpcTarget;
import io.github.kxyr0.echovisibility.api.target.VisibilityTarget;
import java.util.Objects;

public record VisibilityWatchDefinition(
        VisibilityWatchId id,
        VisibilityDefinitionRevision revision,
        VisibilityTarget target,
        VisibilityViewpoint viewpoint,
        VisibilityDetectionMode detectionMode,
        VisibilityOcclusionMode occlusionMode,
        VisibilitySamplePolicy samplePolicy,
        VisibilityHysteresis hysteresis,
        boolean requireVisibleFirst,
        int edgeMarginPixels,
        VisibilityConcealmentPolicy concealmentPolicy,
        VisibilityEvaluationRate evaluationRate,
        String fingerprint) {
    public VisibilityWatchDefinition {
        id = Objects.requireNonNull(id, "id");
        revision = Objects.requireNonNull(revision, "revision");
        target = Objects.requireNonNull(target, "target");
        viewpoint = Objects.requireNonNull(viewpoint, "viewpoint");
        detectionMode = Objects.requireNonNull(detectionMode, "detectionMode");
        occlusionMode = Objects.requireNonNull(occlusionMode, "occlusionMode");
        samplePolicy = Objects.requireNonNull(samplePolicy, "samplePolicy");
        hysteresis = Objects.requireNonNull(hysteresis, "hysteresis");
        concealmentPolicy = Objects.requireNonNull(concealmentPolicy, "concealmentPolicy");
        evaluationRate = Objects.requireNonNull(evaluationRate, "evaluationRate");
        fingerprint = requireFingerprint(fingerprint);
        if (edgeMarginPixels < 0 || edgeMarginPixels > 64) {
            throw new IllegalArgumentException("Edge margin must be within 0..64 pixels.");
        }
        validateTargetPolicy(target, concealmentPolicy, samplePolicy);
    }

    private static void validateTargetPolicy(
            VisibilityTarget target,
            VisibilityConcealmentPolicy concealment,
            VisibilitySamplePolicy samples) {
        if (concealment == VisibilityConcealmentPolicy.HIDE_NPC_IMMEDIATELY
                && !(target instanceof VisibilityNpcTarget)) {
            throw new IllegalArgumentException("Immediate concealment is allowed only for NPC targets.");
        }
        if (target instanceof VisibilityBoxTarget && samples.kinds().stream()
                .anyMatch(kind -> kind.name().startsWith("NPC_"))) {
            throw new IllegalArgumentException("Box target cannot use NPC sample kinds.");
        }
    }

    private static String requireFingerprint(String value) {
        String checked = Objects.requireNonNull(value, "fingerprint").trim();
        if (checked.isEmpty() || checked.length() > 128) {
            throw new IllegalArgumentException("Definition fingerprint must contain 1..128 characters.");
        }
        return checked;
    }
}
