package io.github.kxyr0.echotrigger.api.id;

import java.util.Objects;

public record TriggerOwnerId(String value) {
    public static final TriggerOwnerId SERVER = new TriggerOwnerId("server");

    public TriggerOwnerId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("TriggerOwnerId must not be blank.");
        }
    }
}
