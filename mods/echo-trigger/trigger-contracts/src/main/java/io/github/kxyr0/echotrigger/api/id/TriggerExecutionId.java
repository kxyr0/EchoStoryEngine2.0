package io.github.kxyr0.echotrigger.api.id;

import java.util.Objects;

public record TriggerExecutionId(String value) {
    public TriggerExecutionId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("TriggerExecutionId must not be blank.");
        }
    }
}
