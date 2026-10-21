package io.github.kxyr0.echoanimation.api.animation.query;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackOutcome;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Objects;
import java.util.Optional;

/**
 * Public status snapshot for one high-level animation on one subject.
 */
public record AnimationPlaybackQueryResult(
        Status status,
        AnimationId animationId,
        AnimationSubjectId subjectId,
        Optional<AnimationPlaybackId> playbackId,
        Optional<AnimationPhase> phase,
        Optional<AnimationPlaybackOutcome> outcome,
        long subjectRevision,
        long playbackRevision,
        String diagnosticCode,
        String message
) {
    public AnimationPlaybackQueryResult {
        status = Objects.requireNonNull(status, "status");
        animationId = Objects.requireNonNull(animationId, "animationId");
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        playbackId = Objects.requireNonNull(playbackId, "playbackId");
        phase = Objects.requireNonNull(phase, "phase");
        outcome = Objects.requireNonNull(outcome, "outcome");
        diagnosticCode = Objects.requireNonNull(diagnosticCode, "diagnosticCode");
        message = Objects.requireNonNull(message, "message");
        if (subjectRevision < 0L || playbackRevision < 0L) {
            throw new IllegalArgumentException("Animation playback revisions must be non-negative.");
        }
    }

    public enum Status {
        ACTIVE,
        TERMINAL,
        ANIMATION_NOT_FOUND,
        SUBJECT_NOT_FOUND,
        PLAYBACK_NOT_FOUND,
        AMBIGUOUS_PLAYBACK,
        DEFINITION_UNAVAILABLE
    }
}
