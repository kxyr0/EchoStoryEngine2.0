package io.github.kxyr0.echoaudio.api;

/** Authored provenance retained by portable command receipts and playback status. */
public record AudioSource(String file, int line, int column) {
    public AudioSource {
        java.util.Objects.requireNonNull(file);
        if (line < 1 || column < 1) throw new IllegalArgumentException("Invalid audio source location");
    }
    public static AudioSource api() { return new AudioSource("<api>", 1, 1); }
}
