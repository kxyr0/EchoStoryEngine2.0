package io.github.kxyr0.echodialogue.forge.resource;

import java.util.Objects;

/**
 * UTF-8 decoded source resource.
 */
public record DialogueResourceText(String sourceId, String text) {
    public DialogueResourceText {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        text = Objects.requireNonNull(text, "text");
    }
}
