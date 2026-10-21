package io.github.kxyr0.echodebug.core.identity;

import java.util.Objects;
import java.util.UUID;

public record TriggerWandSessionId(String value) {
    public TriggerWandSessionId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("TriggerWandSessionId is required.");
        }
    }

    public static TriggerWandSessionId random() {
        return new TriggerWandSessionId(UUID.randomUUID().toString());
    }
}
