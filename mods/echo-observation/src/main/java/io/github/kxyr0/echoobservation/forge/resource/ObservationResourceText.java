package io.github.kxyr0.echoobservation.forge.resource;

import java.util.Objects;

record ObservationResourceText(String sourceId, String source) {
    ObservationResourceText {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        source = Objects.requireNonNull(source, "source");
    }
}
