package io.github.kxyr0.echovisibility.api.identity;

import java.util.Objects;

public record VisibilityEventId(String value) implements Comparable<VisibilityEventId> {
    public VisibilityEventId {
        value = require(value);
    }

    @Override
    public int compareTo(VisibilityEventId other) {
        return value.compareTo(other.value);
    }

    private static String require(String value) {
        String checked = Objects.requireNonNull(value, "value").trim();
        if (checked.isEmpty() || checked.length() > 192) {
            throw new IllegalArgumentException("Visibility event id must contain 1..192 characters.");
        }
        return checked;
    }
}
