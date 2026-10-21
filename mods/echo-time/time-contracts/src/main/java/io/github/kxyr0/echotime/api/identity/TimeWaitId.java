package io.github.kxyr0.echotime.api.identity;

import java.util.Objects;

public record TimeWaitId(String value) {
    public TimeWaitId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Time wait ID cannot be blank.");
    }
}
