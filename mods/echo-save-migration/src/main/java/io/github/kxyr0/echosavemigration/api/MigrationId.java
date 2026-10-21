package io.github.kxyr0.echosavemigration.api;

import java.util.Objects;

public record MigrationId(String value) implements Comparable<MigrationId> {
    public MigrationId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid migration id: " + value);
        }
    }

    @Override
    public int compareTo(MigrationId other) {
        return value.compareTo(other.value);
    }
}
