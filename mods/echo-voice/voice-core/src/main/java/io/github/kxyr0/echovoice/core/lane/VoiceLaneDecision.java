package io.github.kxyr0.echovoice.core.lane;

import java.util.List;
import java.util.Objects;

/** Deterministic decision including typed stale/duplicate rejection. */
public record VoiceLaneDecision(Kind kind, List<VoiceLaneAction> actions) {
    public VoiceLaneDecision {
        kind = Objects.requireNonNull(kind, "kind");
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
    }

    static VoiceLaneDecision accepted(VoiceLaneAction... actions) {
        return new VoiceLaneDecision(Kind.ACCEPTED, List.of(actions));
    }

    static VoiceLaneDecision of(Kind kind) {
        return new VoiceLaneDecision(kind, List.of());
    }

    public enum Kind {
        ACCEPTED,
        REJECTED_STALE,
        REJECTED_DUPLICATE,
        NO_OP
    }
}
