package io.github.kxyr0.echoanimation.api.animation;

import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit result for a graceful stop request.
 */
public record AnimationStopResult(
        Status status,
        AnimationSubjectId subjectId,
        Optional<AnimationPlaybackId> playbackId,
        String diagnosticCode,
        String message
) {
    public AnimationStopResult {
        status = Objects.requireNonNull(status, "status");
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        playbackId = Objects.requireNonNull(playbackId, "playbackId");
        diagnosticCode = Objects.requireNonNull(diagnosticCode, "diagnosticCode");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        STOP_REQUESTED,
        ALREADY_APPLIED,
        PLAYBACK_NOT_FOUND,
        SUBJECT_NOT_FOUND,
        AMBIGUOUS_ACTIVE_ANIMATION,
        NOT_STOPPABLE,
        IDEMPOTENCY_CONFLICT
    }
}
