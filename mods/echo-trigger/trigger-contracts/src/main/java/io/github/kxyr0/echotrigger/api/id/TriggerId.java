package io.github.kxyr0.echotrigger.api.id;

import java.util.Objects;

public record TriggerId(String value) {
    public TriggerId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("TriggerId must be namespace:path.");
        }
    }

    public static TriggerId parse(String value) {
        return new TriggerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
