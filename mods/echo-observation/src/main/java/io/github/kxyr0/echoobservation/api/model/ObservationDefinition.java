package io.github.kxyr0.echoobservation.api.model;

import io.github.kxyr0.echoobservation.api.action.ObservationScriptBlock;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ObservationDefinition(
        ObservationId id,
        String symbol,
        Optional<ObservationAnchor> anchor,
        ObservationDuration focus,
        double distance,
        double screenRadius,
        boolean repeatable,
        Optional<String> text,
        Optional<String> thought,
        ObservationScriptBlock onComplete,
        List<String> formerLocalIds,
        long revision,
        ObservationSource source) {
    public ObservationDefinition {
        id = Objects.requireNonNull(id, "id");
        symbol = Objects.requireNonNull(symbol, "symbol");
        anchor = Objects.requireNonNull(anchor, "anchor");
        focus = Objects.requireNonNull(focus, "focus");
        text = Objects.requireNonNull(text, "text");
        thought = Objects.requireNonNull(thought, "thought");
        onComplete = Objects.requireNonNull(onComplete, "onComplete");
        formerLocalIds = List.copyOf(Objects.requireNonNull(formerLocalIds, "formerLocalIds"));
        source = Objects.requireNonNull(source, "source");
        if (symbol.isBlank() || !Double.isFinite(distance) || distance <= 0.0D || distance > 128.0D
                || !Double.isFinite(screenRadius) || screenRadius < 4.0D || screenRadius > 128.0D || revision < 0L) {
            throw new IllegalArgumentException("Invalid Observation definition.");
        }
    }

    public boolean placed() {
        return anchor.isPresent();
    }
}
