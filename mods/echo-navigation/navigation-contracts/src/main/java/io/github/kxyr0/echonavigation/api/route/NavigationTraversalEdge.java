package io.github.kxyr0.echonavigation.api.route;

import java.util.Objects;

public record NavigationTraversalEdge(
        int fromNodeIndex,
        int toNodeIndex,
        NavigationTraversalType type,
        int entryHeight16,
        int exitHeight16,
        double cost) {
    public NavigationTraversalEdge {
        type = Objects.requireNonNull(type, "type");
        if (fromNodeIndex < 0 || toNodeIndex < 0 || cost <= 0.0D || !Double.isFinite(cost)) {
            throw new IllegalArgumentException("Traversal edge is invalid.");
        }
    }
}
