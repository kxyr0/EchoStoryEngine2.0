package io.github.kxyr0.echothoughtspace.api;

import java.util.Objects;

public record ThoughtNodeLabel(String value) {
    public ThoughtNodeLabel {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Thought node label must not be blank.");
    }
}

