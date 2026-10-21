package io.github.kxyr0.echoaudio.api;

import java.util.UUID;

public record AudioPlaybackId(UUID value) {
    public AudioPlaybackId { java.util.Objects.requireNonNull(value); }
    public static AudioPlaybackId next() { return new AudioPlaybackId(UUID.randomUUID()); }
}
