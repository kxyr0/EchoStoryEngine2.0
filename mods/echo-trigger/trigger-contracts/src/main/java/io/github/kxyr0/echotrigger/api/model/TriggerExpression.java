package io.github.kxyr0.echotrigger.api.model;

import java.util.Objects;

public record TriggerExpression(String source, TriggerCondition condition) {
    public TriggerExpression {
        source = Objects.requireNonNull(source, "source").trim();
        condition = Objects.requireNonNull(condition, "condition");
        if (source.isEmpty()) {
            throw new IllegalArgumentException("TriggerExpression source must not be blank.");
        }
    }

    public String fingerprint() {
        return Integer.toHexString(source.hashCode());
    }
}
