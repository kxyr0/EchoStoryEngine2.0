package io.github.kxyr0.echodialogue.api.identity;

import java.util.Objects;

/**
 * Stable identifier of a compiled dialogue.
 */
public record DialogueId(NamespacedId value) {
    public DialogueId {
        value = Objects.requireNonNull(value, "value");
    }

    public static DialogueId parse(String raw) {
        return new DialogueId(new NamespacedId(raw));
    }

    @Override
    public String toString() {
        return value.value();
    }
}
