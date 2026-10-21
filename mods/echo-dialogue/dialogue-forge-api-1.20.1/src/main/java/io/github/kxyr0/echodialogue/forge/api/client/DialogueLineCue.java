package io.github.kxyr0.echodialogue.forge.api.client;

import java.util.Objects;

/** Client-safe immutable Line cue. */
public record DialogueLineCue(String name, String value) {
    public DialogueLineCue {
        name = Objects.requireNonNull(name, "name");
        value = Objects.requireNonNull(value, "value");
    }
}
