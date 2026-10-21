package io.github.kxyr0.echoevents.api.payload;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import java.util.Objects;
import java.util.Optional;

public record SceneFact(Phase phase, Optional<SymbolId> failure) implements EventPayload {
    public SceneFact {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(failure, "failure");
        if ((phase == Phase.FAILED) != failure.isPresent()) throw new IllegalArgumentException("Failure code required");
    }
    public EventKind kind() {
        return switch (phase) {
            case STARTED -> EventKind.SCENE_STARTED;
            case COMPLETED -> EventKind.SCENE_COMPLETED;
            case CANCELLED -> EventKind.SCENE_CANCELLED;
            case FAILED -> EventKind.SCENE_FAILED;
        };
    }
    public enum Phase { STARTED, COMPLETED, CANCELLED, FAILED }
}
