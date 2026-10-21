package io.github.kxyr0.echoiteminspection.model;

import java.util.Objects;

/** Stable namespaced resource identity, independent of the authored class name. */
public record ItemInspectionId(String value) implements Comparable<ItemInspectionId> {
    public ItemInspectionId {
        Objects.requireNonNull(value, "value");
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("Invalid inspection resource ID: " + value);
        }
    }

    @Override
    public int compareTo(ItemInspectionId other) { return value.compareTo(other.value); }

    @Override
    public String toString() { return value; }
}
