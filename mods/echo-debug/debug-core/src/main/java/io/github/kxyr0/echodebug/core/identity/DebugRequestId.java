package io.github.kxyr0.echodebug.core.identity;

import java.util.Objects;
import java.util.UUID;

public record DebugRequestId(String value) {
    public DebugRequestId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("DebugRequestId is required.");
        }
    }

    public static DebugRequestId random() {
        return new DebugRequestId(UUID.randomUUID().toString());
    }
}
