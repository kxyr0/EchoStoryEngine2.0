package io.github.kxyr0.echonpcbackpack.api.world;

import java.util.Objects;

public record DimensionId(String value) {
    public DimensionId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("DimensionId must be namespaced.");
        }
    }
}
