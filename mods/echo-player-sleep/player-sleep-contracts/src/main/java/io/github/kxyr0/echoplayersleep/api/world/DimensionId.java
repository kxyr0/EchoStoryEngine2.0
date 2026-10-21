package io.github.kxyr0.echoplayersleep.api.world;

import java.util.Objects;
import java.util.regex.Pattern;

public record DimensionId(String value) {
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public DimensionId {
        value = requireId(value, "dimensionId");
    }

    private static String requireId(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (!ID.matcher(checked).matches()) {
            throw new IllegalArgumentException(name + " must be a namespaced id");
        }
        return checked;
    }
}
