package io.github.kxyr0.echothoughtspace.core.model;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeSnapshot;
import io.github.kxyr0.echothoughtspace.api.ThoughtCanvasPosition;
import java.util.Objects;

public record ThoughtCanvasNode(ThoughtNodeSnapshot snapshot, ThoughtCanvasPosition position) {
    public ThoughtCanvasNode {
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        position = Objects.requireNonNull(position, "position");
    }
}
