package io.github.kxyr0.echovoice.api.identity;

/** Monotonic revision of one successfully committed asset key. */
public record VoiceAssetRevision(long value) implements Comparable<VoiceAssetRevision> {
    public VoiceAssetRevision {
        if (value < 1) {
            throw new IllegalArgumentException("Voice asset revision must be positive.");
        }
    }

    @Override
    public int compareTo(VoiceAssetRevision other) {
        return Long.compare(value, other.value);
    }
}
