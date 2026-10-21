package io.github.kxyr0.echoitemattachments.core.model;

import java.util.Objects;

public record ReleasePlan(
        String dimensionId,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        double velocityZ) {
    public ReleasePlan {
        dimensionId = Objects.requireNonNull(dimensionId, "dimension id").trim();
        if (!dimensionId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("dimension id must be namespaced");
        }
        if (!finite(x, y, z, velocityX, velocityY, velocityZ)) {
            throw new IllegalArgumentException("release plan values must be finite");
        }
    }

    private static boolean finite(double... values) {
        for (double value : values) if (!Double.isFinite(value)) return false;
        return true;
    }
}
