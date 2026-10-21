package io.github.kxyr0.echovoice.api.identity;

/** Monotonic local command generation used to reject stale asynchronous starts. */
public record VoicePlaybackGeneration(long value) implements Comparable<VoicePlaybackGeneration> {
    public VoicePlaybackGeneration {
        if (value < 0) {
            throw new IllegalArgumentException("Voice playback generation must not be negative.");
        }
    }

    public VoicePlaybackGeneration next() {
        return new VoicePlaybackGeneration(value + 1);
    }

    @Override
    public int compareTo(VoicePlaybackGeneration other) {
        return Long.compare(value, other.value);
    }
}
