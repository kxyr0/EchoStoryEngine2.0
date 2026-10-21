package io.github.kxyr0.echosavemigration.api;

public record SaveSchemaVersion(int value) implements Comparable<SaveSchemaVersion> {
    public SaveSchemaVersion {
        if (value < 0) {
            throw new IllegalArgumentException("Save schema version must not be negative.");
        }
    }

    @Override
    public int compareTo(SaveSchemaVersion other) {
        return Integer.compare(value, other.value);
    }
}
