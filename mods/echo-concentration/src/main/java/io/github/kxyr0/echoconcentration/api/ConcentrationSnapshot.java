package io.github.kxyr0.echoconcentration.api;

import java.util.Objects;

public record ConcentrationSnapshot(ConcentrationState state, ConcentrationRevision revision) {
    public ConcentrationSnapshot {
        state = Objects.requireNonNull(state, "state");
        revision = Objects.requireNonNull(revision, "revision");
    }

    public boolean active() {
        return state == ConcentrationState.ACTIVE;
    }
}
