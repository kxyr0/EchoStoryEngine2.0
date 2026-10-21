package io.github.kxyr0.echoanimation.api.animation;

import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result for a high-level play request.
 */
public record AnimationPlayResult(
        Status status,
        AnimationId animationId,
        AnimationSubjectId subjectId,
        Optional<AnimationPlaybackId> playbackId,
        Optional<AnimationPhase> initialPhase,
        String diagnosticCode,
        String message
) {
    public AnimationPlayResult {
        status = Objects.requireNonNull(status, "status");
        animationId = Objects.requireNonNull(animationId, "animationId");
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        playbackId = Objects.requireNonNull(playbackId, "playbackId");
        initialPhase = Objects.requireNonNull(initialPhase, "initialPhase");
        diagnosticCode = Objects.requireNonNull(diagnosticCode, "diagnosticCode");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        STARTED,
        ALREADY_APPLIED,
        ANIMATION_NOT_FOUND,
        SUBJECT_NOT_FOUND,
        INVALID_SOURCE_STATE,
        CHANNEL_CONFLICT,
        LEASE_CONFLICT,
        DEFINITION_UNAVAILABLE,
        IDEMPOTENCY_CONFLICT
    }
}
