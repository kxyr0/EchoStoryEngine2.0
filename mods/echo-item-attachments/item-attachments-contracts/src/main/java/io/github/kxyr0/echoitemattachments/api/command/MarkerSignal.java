package io.github.kxyr0.echoitemattachments.api.command;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.Objects;

public record MarkerSignal(
        AttachmentSubjectRef subject,
        String animationId,
        String playbackId,
        long playbackRevision,
        String phase,
        String markerId,
        long serverTick) {
    public MarkerSignal {
        subject = Objects.requireNonNull(subject, "subject");
        animationId = required(animationId, "animation id");
        playbackId = required(playbackId, "playback id");
        phase = required(phase, "phase");
        markerId = required(markerId, "marker id");
        if (playbackRevision < 1 || serverTick < 0) {
            throw new IllegalArgumentException("revision and tick must be non-negative");
        }
    }

    private static String required(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must be non-empty");
        return normalized;
    }
}
