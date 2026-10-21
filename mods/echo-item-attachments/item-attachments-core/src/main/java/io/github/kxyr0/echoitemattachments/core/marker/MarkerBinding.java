package io.github.kxyr0.echoitemattachments.core.marker;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.Objects;

public record MarkerBinding(
        AttachmentCommandId commandId,
        AttachmentSubjectRef subject,
        String animationId,
        String playbackId,
        long playbackRevision,
        String phase,
        String markerId) {
    public MarkerBinding {
        commandId = Objects.requireNonNull(commandId, "command id");
        subject = Objects.requireNonNull(subject, "subject");
        animationId = required(animationId, "animation id");
        playbackId = required(playbackId, "playback id");
        phase = required(phase, "phase");
        markerId = required(markerId, "marker id");
        if (playbackRevision < 1) throw new IllegalArgumentException("playback revision must be positive");
    }

    private static String required(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must be non-empty");
        return normalized;
    }
}
