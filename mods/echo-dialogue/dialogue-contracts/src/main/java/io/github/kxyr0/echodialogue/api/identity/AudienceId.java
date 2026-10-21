package io.github.kxyr0.echodialogue.api.identity;

import java.util.Objects;

/**
 * Neutral audience/player identifier. Forge maps this to a concrete server player outside core.
 */
public record AudienceId(String value) {
    public AudienceId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("ID аудитории диалога не может быть пустым.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
