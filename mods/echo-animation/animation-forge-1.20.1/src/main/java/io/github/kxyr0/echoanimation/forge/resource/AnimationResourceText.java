package io.github.kxyr0.echoanimation.forge.resource;

import java.util.Objects;

/**
 * UTF-8 decoded resource text.
 */
public record AnimationResourceText(String sourceId, String text) {
    public AnimationResourceText {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        text = Objects.requireNonNull(text, "text");
    }
}

