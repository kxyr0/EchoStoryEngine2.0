package io.github.kxyr0.echovoice.forge.playback.lane;

import java.util.Objects;

/** Last accepted foreground decode result for stable diagnostics. */
public record VoiceDecodeMetrics(String decoder, long latencyMillis, String cacheResult) {
    public VoiceDecodeMetrics {
        decoder = Objects.requireNonNull(decoder, "decoder");
        cacheResult = Objects.requireNonNull(cacheResult, "cacheResult");
    }

    static VoiceDecodeMetrics none() {
        return new VoiceDecodeMetrics("NONE", 0, "NONE");
    }
}
