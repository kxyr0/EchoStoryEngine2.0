package io.github.kxyr0.echonpcbackpack.core.transition;

import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPhase;
import java.util.Objects;

public record BackpackTransitionDecision(Status status, NpcBackpackTransitionPhase phase, String diagnosticCode) {
    public BackpackTransitionDecision {
        status = Objects.requireNonNull(status, "status");
        phase = Objects.requireNonNull(phase, "phase");
        diagnosticCode = Objects.requireNonNull(diagnosticCode, "diagnosticCode");
    }

    public enum Status {
        ADVANCED,
        DUPLICATE,
        OUT_OF_ORDER,
        TERMINAL
    }
}
