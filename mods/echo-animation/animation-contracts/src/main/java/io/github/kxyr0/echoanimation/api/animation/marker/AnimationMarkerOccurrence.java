package io.github.kxyr0.echoanimation.api.animation.marker;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Objects;

/**
 * Neutral immutable payload for one server marker delivery.
 */
public record AnimationMarkerOccurrence(
        AnimationSubjectId subjectId,
        AnimationId animationId,
        AnimationPlaybackId playbackId,
        AnimationOwnerId ownerId,
        AnimationMarkerId markerId,
        AnimationPhase phase,
        long phaseTick,
        long serverTick,
        long playbackRevision
) {
    public AnimationMarkerOccurrence {
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        animationId = Objects.requireNonNull(animationId, "animationId");
        playbackId = Objects.requireNonNull(playbackId, "playbackId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        markerId = Objects.requireNonNull(markerId, "markerId");
        phase = Objects.requireNonNull(phase, "phase");
        if (phaseTick <= 0L || serverTick < 0L || playbackRevision < 1L) {
            throw new IllegalArgumentException("Marker occurrence ticks/revision are outside supported bounds.");
        }
    }
}
