package io.github.kxyr0.echoitemattachments.api.identity;

import java.util.Objects;

public record ReleaseId(String value) implements Comparable<ReleaseId> {
    public ReleaseId {
        value = Objects.requireNonNull(value, "release id").trim();
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("release id must be namespaced: " + value);
        }
    }

    @Override
    public int compareTo(ReleaseId other) {
        return value.compareTo(other.value);
    }
}
