package io.github.kxyr0.echovisibility.api.identity;

import java.util.Objects;

public record VisibilityWatchId(String value) implements Comparable<VisibilityWatchId> {
    public VisibilityWatchId {
        value = requireId(value, "value");
    }

    @Override
    public int compareTo(VisibilityWatchId other) {
        return value.compareTo(other.value);
    }

    private static String requireId(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (!checked.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(name + " must be namespace:path.");
        }
        return checked;
    }
}
