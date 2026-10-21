package io.github.kxyr0.echoobservation.api.identity;

import java.util.Objects;

public record ObservationId(ObservationResourceId resource, ObservationLocalId local)
        implements Comparable<ObservationId> {
    public ObservationId {
        resource = Objects.requireNonNull(resource, "resource");
        local = Objects.requireNonNull(local, "local");
    }

    public static ObservationId parse(String value) {
        int separator = Objects.requireNonNull(value, "value").lastIndexOf('#');
        if (separator < 1 || separator == value.length() - 1) {
            throw new IllegalArgumentException("Observation ID must contain resource#local: " + value);
        }
        return new ObservationId(ObservationResourceId.parse(value.substring(0, separator)),
                new ObservationLocalId(value.substring(separator + 1)));
    }

    @Override
    public String toString() {
        return resource + "#" + local;
    }

    @Override
    public int compareTo(ObservationId other) {
        return toString().compareTo(other.toString());
    }
}
