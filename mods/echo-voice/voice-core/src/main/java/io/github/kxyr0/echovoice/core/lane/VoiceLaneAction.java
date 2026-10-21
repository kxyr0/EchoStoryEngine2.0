package io.github.kxyr0.echovoice.core.lane;

import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;
import java.util.Objects;
import java.util.Optional;

/** One explicit side effect requested by the pure lane state machine. */
public record VoiceLaneAction(Kind kind, VoicePresentationToken token, Optional<VoiceStopReason> stopReason) {
    public VoiceLaneAction {
        kind = Objects.requireNonNull(kind, "kind");
        token = Objects.requireNonNull(token, "token");
        stopReason = Objects.requireNonNull(stopReason, "stopReason");
    }

    static VoiceLaneAction start(VoicePresentationToken token) {
        return new VoiceLaneAction(Kind.START, token, Optional.empty());
    }

    static VoiceLaneAction stop(VoicePresentationToken token, VoiceStopReason reason) {
        return new VoiceLaneAction(Kind.STOP, token, Optional.of(reason));
    }

    public enum Kind {
        START,
        STOP
    }
}
