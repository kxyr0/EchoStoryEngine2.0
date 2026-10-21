package io.github.kxyr0.echodialogue.echoscript.api;

import java.util.Objects;

/** Neutral immutable cue contributed to a compiled dialogue Line. */
public record DialogueLineExtensionCue(String name, String value) {
    public DialogueLineExtensionCue {
        name = requireText(name, "name");
        value = requireText(value, "value");
    }

    private static String requireText(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return checked;
    }
}
