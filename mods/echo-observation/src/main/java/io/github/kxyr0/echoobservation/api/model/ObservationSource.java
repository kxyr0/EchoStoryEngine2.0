package io.github.kxyr0.echoobservation.api.model;

import java.util.Objects;
import java.util.Optional;

public record ObservationSource(
        String sourceId,
        int declarationStart,
        int declarationEnd,
        int bodyStart,
        Optional<Span> atSpan,
        int line) {
    public ObservationSource {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        atSpan = Objects.requireNonNull(atSpan, "atSpan");
        if (sourceId.isBlank() || declarationStart < 0 || declarationEnd < declarationStart
                || bodyStart < declarationStart || bodyStart >= declarationEnd || line < 1) {
            throw new IllegalArgumentException("Invalid Observation source coordinates.");
        }
    }

    public record Span(int start, int end) {
        public Span {
            if (start < 0 || end < start) {
                throw new IllegalArgumentException("Invalid source span.");
            }
        }
    }
}
