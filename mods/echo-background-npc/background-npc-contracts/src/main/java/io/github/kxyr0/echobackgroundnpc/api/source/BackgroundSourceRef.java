package io.github.kxyr0.echobackgroundnpc.api.source;

import java.util.Objects;

/** Source-linked provenance retained through scheduling and dialogue playback. */
public record BackgroundSourceRef(String sourceId, int line, int column) {
    public BackgroundSourceRef {
        sourceId = Objects.requireNonNull(sourceId, "sourceId").trim();
        if (sourceId.isEmpty() || line < 1 || column < 1) {
            throw new IllegalArgumentException("Source reference is invalid.");
        }
    }
}
