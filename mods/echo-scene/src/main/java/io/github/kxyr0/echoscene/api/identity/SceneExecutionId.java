package io.github.kxyr0.echoscene.api.identity;

import java.util.Objects;

public record SceneExecutionId(String value) implements Comparable<SceneExecutionId> {
    public SceneExecutionId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Scene execution id must not be empty.");
        }
    }

    @Override
    public int compareTo(SceneExecutionId other) {
        return value.compareTo(other.value);
    }
}
