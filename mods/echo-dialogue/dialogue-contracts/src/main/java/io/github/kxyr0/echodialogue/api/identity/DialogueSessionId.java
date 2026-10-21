package io.github.kxyr0.echodialogue.api.identity;

import java.util.Objects;

/**
 * Stable server-issued session identifier.
 */
public record DialogueSessionId(String value) {
    public DialogueSessionId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("ID сессии диалога не может быть пустым.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
