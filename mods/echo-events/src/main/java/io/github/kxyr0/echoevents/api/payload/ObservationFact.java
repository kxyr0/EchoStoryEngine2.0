package io.github.kxyr0.echoevents.api.payload;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import java.util.Objects;

public record ObservationFact(SymbolId completion) implements EventPayload {
    public ObservationFact { Objects.requireNonNull(completion, "completion"); }
    public EventKind kind() { return EventKind.OBSERVATION_COMPLETED; }
}
