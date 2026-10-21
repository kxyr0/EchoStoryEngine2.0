package io.github.kxyr0.echosavemigration.api;

import java.util.Objects;

public record ResourceId(String value) implements Comparable<ResourceId> {
    public ResourceId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid resource id: " + value);
        }
    }

    @Override
    public int compareTo(ResourceId other) {
        return value.compareTo(other.value);
    }
}
