package io.github.kxyr0.echoscene.api.identity;

import java.util.Objects;

public record SceneStepId(String value) implements Comparable<SceneStepId> {
    public SceneStepId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!value.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid scene step id: " + value);
        }
    }

    @Override
    public int compareTo(SceneStepId other) {
        return value.compareTo(other.value);
    }
}
