package io.github.kxyr0.echodialogue.api.outcome;

import io.github.kxyr0.echodialogue.api.identity.NamespacedId;
import java.util.Objects;

/**
 * Stable source-authored terminal dialogue outcome.
 */
public record DialogueOutcomeId(NamespacedId value) {
    public static final DialogueOutcomeId DEFAULT = parse("echo_dialogue:completed");

    public DialogueOutcomeId {
        value = Objects.requireNonNull(value, "value");
    }

    public static DialogueOutcomeId parse(String raw) {
        return new DialogueOutcomeId(new NamespacedId(raw));
    }

    @Override
    public String toString() {
        return value.value();
    }
}
