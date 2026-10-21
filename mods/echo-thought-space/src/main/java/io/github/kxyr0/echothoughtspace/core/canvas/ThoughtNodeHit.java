package io.github.kxyr0.echothoughtspace.core.canvas;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeId;
import java.util.Objects;

public record ThoughtNodeHit(ThoughtNodeId nodeId, double distanceSquared, int drawOrder) {
    public ThoughtNodeHit {
        nodeId = Objects.requireNonNull(nodeId, "nodeId");
        if (!Double.isFinite(distanceSquared) || distanceSquared < 0.0D || drawOrder < 0) {
            throw new IllegalArgumentException("Invalid node hit.");
        }
    }
}

