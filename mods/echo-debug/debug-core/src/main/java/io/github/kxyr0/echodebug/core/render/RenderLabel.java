package io.github.kxyr0.echodebug.core.render;

import io.github.kxyr0.echotrigger.api.geometry.Vector3;
import java.util.Objects;

public record RenderLabel(Vector3 position, String text) {
    public RenderLabel {
        position = Objects.requireNonNull(position, "position");
        text = Objects.requireNonNull(text, "text").trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Render label text is required.");
        }
    }
}
