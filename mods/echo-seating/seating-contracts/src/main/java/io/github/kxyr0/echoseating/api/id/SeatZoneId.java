package io.github.kxyr0.echoseating.api.id;

import java.util.Objects;

public record SeatZoneId(String value) {
    public SeatZoneId {
        value = requireId(value, "SeatZoneId");
    }

    private static String requireId(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (!checked.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(name + " must be namespace:path.");
        }
        return checked;
    }
}
