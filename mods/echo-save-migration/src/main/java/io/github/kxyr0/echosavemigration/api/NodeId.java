package io.github.kxyr0.echosavemigration.api;

import java.util.Objects;

public record NodeId(String value) {
    public NodeId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!value.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid node id: " + value);
        }
    }
}
