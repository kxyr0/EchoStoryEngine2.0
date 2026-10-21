package io.github.kxyr0.echoquests.api.source;

import java.util.Objects;

public record QuestSourceRef(String sourceId, int line, int column) {
    public QuestSourceRef {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        if (line < 1 || column < 1) throw new IllegalArgumentException("Source position must be positive.");
    }
}
