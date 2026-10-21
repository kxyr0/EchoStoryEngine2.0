package io.github.kxyr0.echoitemattachments.api.model;

import java.util.Objects;

public record WorldPosition(String dimensionId, double x, double y, double z) {
    public WorldPosition {
        dimensionId = Objects.requireNonNull(dimensionId, "dimension id").trim();
        if (!dimensionId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("dimension id must be namespaced");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("position must be finite");
        }
    }
}
