package io.github.kxyr0.echoevents.api.payload;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import java.util.Objects;
import java.util.Optional;

public record DialogueFact(Phase phase, Optional<SymbolId> choice, Optional<SymbolId> failure)
        implements EventPayload {
    public DialogueFact {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(choice, "choice");
        Objects.requireNonNull(failure, "failure");
        if ((phase == Phase.CHOICE) != choice.isPresent() || (phase == Phase.FAILED) != failure.isPresent()) {
            throw new IllegalArgumentException("Dialogue payload does not match phase");
        }
    }
    public static DialogueFact lifecycle(Phase phase) {
        return new DialogueFact(phase, Optional.empty(), Optional.empty());
    }
    public static DialogueFact choice(SymbolId choice) {
        return new DialogueFact(Phase.CHOICE, Optional.of(choice), Optional.empty());
    }
    public static DialogueFact failed(SymbolId code) {
        return new DialogueFact(Phase.FAILED, Optional.empty(), Optional.of(code));
    }
    public EventKind kind() {
        return switch (phase) {
            case STARTED -> EventKind.DIALOGUE_STARTED;
            case CHOICE -> EventKind.DIALOGUE_CHOICE;
            case COMPLETED -> EventKind.DIALOGUE_COMPLETED;
            case CANCELLED -> EventKind.DIALOGUE_CANCELLED;
            case FAILED -> EventKind.DIALOGUE_FAILED;
        };
    }
    public enum Phase { STARTED, CHOICE, COMPLETED, CANCELLED, FAILED }
}
