package io.github.kxyr0.echoquests.echoscript;

import java.util.Objects;

public record QuestScriptSource(String sourceId, String text) {
    public QuestScriptSource {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        text = Objects.requireNonNull(text, "text");
    }
}
