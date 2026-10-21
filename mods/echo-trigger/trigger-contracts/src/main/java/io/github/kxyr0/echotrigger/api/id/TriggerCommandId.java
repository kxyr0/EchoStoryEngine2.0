package io.github.kxyr0.echotrigger.api.id;

import java.util.Objects;

public record TriggerCommandId(String value) {
    public TriggerCommandId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("TriggerCommandId must not be blank.");
        }
    }
}
