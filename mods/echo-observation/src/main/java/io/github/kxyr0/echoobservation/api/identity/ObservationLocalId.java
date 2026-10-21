package io.github.kxyr0.echoobservation.api.identity;

import java.util.Objects;
import java.util.regex.Pattern;

public record ObservationLocalId(String value) implements Comparable<ObservationLocalId> {
    private static final Pattern VALID = Pattern.compile("[a-z0-9_./-]+");

    public ObservationLocalId {
        value = Objects.requireNonNull(value, "value");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid local Observation ID: " + value);
        }
    }

    @Override
    public int compareTo(ObservationLocalId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
