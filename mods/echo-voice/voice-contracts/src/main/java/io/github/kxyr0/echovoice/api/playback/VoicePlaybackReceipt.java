package io.github.kxyr0.echovoice.api.playback;

import io.github.kxyr0.echovoice.api.identity.VoicePlaybackId;
import java.util.Objects;
import java.util.Optional;

/** Immutable acknowledgement or terminal outcome for a playback attempt. */
public record VoicePlaybackReceipt(
        VoicePlaybackId playbackId,
        VoicePlaybackOutcome outcome,
        Optional<VoiceStopReason> stopReason,
        long playedMillis,
        String detail
) {
    public VoicePlaybackReceipt {
        playbackId = Objects.requireNonNull(playbackId, "playbackId");
        outcome = Objects.requireNonNull(outcome, "outcome");
        stopReason = Objects.requireNonNull(stopReason, "stopReason");
        detail = Objects.requireNonNull(detail, "detail");
    }
}
