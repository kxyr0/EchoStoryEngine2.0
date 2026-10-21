package io.github.kxyr0.echoseating.forge.resource;

import java.util.Objects;

public record SeatZoneResourceText(String sourceId, String text) {
    public SeatZoneResourceText {
        sourceId = Objects.requireNonNull(sourceId, "sourceId").trim();
        text = Objects.requireNonNull(text, "text");
        if (sourceId.isEmpty()) {
            throw new IllegalArgumentException("sourceId must not be blank.");
        }
    }
}
