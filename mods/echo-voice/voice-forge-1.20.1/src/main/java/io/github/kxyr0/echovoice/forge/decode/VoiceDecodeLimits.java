package io.github.kxyr0.echovoice.forge.decode;

public record VoiceDecodeLimits(long maximumFileBytes, long maximumDurationMillis) {
    public VoiceDecodeLimits {
        if (maximumFileBytes < 1 || maximumDurationMillis < 1) {
            throw new IllegalArgumentException("Voice decode limits must be positive.");
        }
    }
}
