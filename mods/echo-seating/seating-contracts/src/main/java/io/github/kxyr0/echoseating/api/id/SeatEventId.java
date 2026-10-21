package io.github.kxyr0.echoseating.api.id;

import java.util.Objects;

public record SeatEventId(String value) {
    public SeatEventId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("SeatEventId must not be blank.");
        }
    }
}
