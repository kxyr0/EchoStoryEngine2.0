package io.github.kxyr0.echoevents.api.payload;

import io.github.kxyr0.echoevents.api.catalog.EventKind;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import java.util.Objects;
import java.util.Optional;

public record AnimationFact(SymbolId subject, SymbolId playback, Optional<SymbolId> marker)
        implements EventPayload {
    public AnimationFact {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(playback, "playback");
        Objects.requireNonNull(marker, "marker");
    }
    public EventKind kind() { return marker.isPresent() ? EventKind.ANIMATION_MARKER : EventKind.ANIMATION_COMPLETED; }
}
