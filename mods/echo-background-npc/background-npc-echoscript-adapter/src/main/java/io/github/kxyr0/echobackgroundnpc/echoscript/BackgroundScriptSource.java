package io.github.kxyr0.echobackgroundnpc.echoscript;

import java.util.Objects;

public record BackgroundScriptSource(String sourceId, String text) {
    public BackgroundScriptSource {
        sourceId = Objects.requireNonNull(sourceId, "sourceId").trim();
        text = Objects.requireNonNull(text, "text");
        if (sourceId.isEmpty()) throw new IllegalArgumentException("sourceId cannot be blank.");
    }
}
