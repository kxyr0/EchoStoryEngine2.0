package io.github.kxyr0.echoobservation.api.identity;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record ObservationResourceId(String namespace, String path) implements Comparable<ObservationResourceId> {
    private static final Pattern PART = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

    public ObservationResourceId {
        namespace = Objects.requireNonNull(namespace, "namespace").toLowerCase(Locale.ROOT);
        path = Objects.requireNonNull(path, "path").toLowerCase(Locale.ROOT);
        if (!PART.matcher(namespace).matches() || !PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid Observation resource ID: " + namespace + ":" + path);
        }
    }

    public static ObservationResourceId parse(String value) {
        int separator = Objects.requireNonNull(value, "value").indexOf(':');
        if (separator < 1 || separator == value.length() - 1) {
            throw new IllegalArgumentException("Observation resource ID must be namespaced: " + value);
        }
        return new ObservationResourceId(value.substring(0, separator), value.substring(separator + 1));
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public int compareTo(ObservationResourceId other) {
        return toString().compareTo(other.toString());
    }
}
