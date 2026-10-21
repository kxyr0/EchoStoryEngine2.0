package io.github.kxyr0.echoevents.api.payload;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import java.util.Objects;

public record CutsceneFact(Phase phase) implements EventPayload {
    public CutsceneFact { Objects.requireNonNull(phase, "phase"); }
    public EventKind kind() {
        return switch (phase) {
            case STARTED -> EventKind.CUTSCENE_STARTED;
            case COMPLETED -> EventKind.CUTSCENE_COMPLETED;
            case CANCELLED -> EventKind.CUTSCENE_CANCELLED;
        };
    }
    public enum Phase { STARTED, COMPLETED, CANCELLED }
}
