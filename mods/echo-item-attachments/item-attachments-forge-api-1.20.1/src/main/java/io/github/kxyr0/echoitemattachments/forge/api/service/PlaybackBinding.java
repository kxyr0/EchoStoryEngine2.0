package io.github.kxyr0.echoitemattachments.forge.api.service;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import java.util.Objects;

public record PlaybackBinding(
        AttachmentCommandId commandId,
        String animationId,
        String playbackId,
        long playbackRevision,
        String phase,
        String markerId) {
    public PlaybackBinding {
        commandId = Objects.requireNonNull(commandId, "command id");
        animationId = required(animationId, "animation id");
        playbackId = required(playbackId, "playback id");
        phase = required(phase, "phase");
        markerId = required(markerId, "marker id");
        if (playbackRevision < 1) throw new IllegalArgumentException("revision must be positive");
    }

    private static String required(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " required");
        return normalized;
    }
}
