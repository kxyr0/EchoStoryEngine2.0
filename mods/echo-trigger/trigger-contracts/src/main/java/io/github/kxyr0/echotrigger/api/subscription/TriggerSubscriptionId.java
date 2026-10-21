package io.github.kxyr0.echotrigger.api.subscription;

import java.util.Objects;

public record TriggerSubscriptionId(String value) {
    public TriggerSubscriptionId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("TriggerSubscriptionId must not be blank.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
