package io.github.kxyr0.echonpcanimationadapter.resource;

import java.util.Objects;

public record NpcAnimationBindingResourceText(String sourceId, String text) {
    public NpcAnimationBindingResourceText {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        text = Objects.requireNonNull(text, "text");
    }
}
