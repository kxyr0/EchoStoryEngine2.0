package io.github.kxyr0.echoaudio.api;

public record AudioRevision(String sha256) {
    public AudioRevision { java.util.Objects.requireNonNull(sha256); }
}
