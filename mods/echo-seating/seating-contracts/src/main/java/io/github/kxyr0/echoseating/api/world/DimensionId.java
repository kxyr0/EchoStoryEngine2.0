package io.github.kxyr0.echoseating.api.world;

import java.util.Objects;

public record DimensionId(String value) {
    public DimensionId {
        value = requireId(value, "DimensionId");
    }

    private static String requireId(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (!checked.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException(name + " must be namespace:path.");
        }
        return checked;
    }
}
