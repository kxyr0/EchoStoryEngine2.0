package io.github.kxyr0.echoitemattachments.api.identity;

import java.util.Objects;

public record CustodyId(String value) implements Comparable<CustodyId> {
    public CustodyId {
        value = Objects.requireNonNull(value, "custody id").trim();
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("custody id must be namespaced: " + value);
        }
    }

    @Override
    public int compareTo(CustodyId other) {
        return value.compareTo(other.value);
    }
}
