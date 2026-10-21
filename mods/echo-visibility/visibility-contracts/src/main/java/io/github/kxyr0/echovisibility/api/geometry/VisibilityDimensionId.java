package io.github.kxyr0.echovisibility.api.geometry;

import java.util.Objects;

public record VisibilityDimensionId(String value) implements Comparable<VisibilityDimensionId> {
    public VisibilityDimensionId {
        String checked = Objects.requireNonNull(value, "value").trim();
        if (!checked.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Dimension id must be namespace:path.");
        }
        value = checked;
    }

    @Override
    public int compareTo(VisibilityDimensionId other) {
        return value.compareTo(other.value);
    }
}
