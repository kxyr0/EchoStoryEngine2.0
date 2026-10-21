package io.github.kxyr0.echotrigger.api.id;

import java.util.Objects;

public record TriggerInstanceId(String value) {
    public TriggerInstanceId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("TriggerInstanceId must not be blank.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
