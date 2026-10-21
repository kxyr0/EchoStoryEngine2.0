package io.github.kxyr0.echoquests.forge.resource;

import java.util.Objects;

record QuestResourceText(String sourceId, String text) {
    QuestResourceText {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        text = Objects.requireNonNull(text, "text");
    }
}
