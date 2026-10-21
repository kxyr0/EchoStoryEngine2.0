package io.github.kxyr0.echovoice.api.identity;

import java.util.Objects;
import java.util.UUID;

/** Unique local playback attempt identifier. */
public record VoicePlaybackId(String value) {
    public VoicePlaybackId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Voice playback ID must not be blank.");
        }
    }

    public static VoicePlaybackId next() {
        return new VoicePlaybackId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
