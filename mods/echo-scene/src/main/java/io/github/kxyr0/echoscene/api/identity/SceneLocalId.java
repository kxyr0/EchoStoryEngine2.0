package io.github.kxyr0.echoscene.api.identity;

import java.util.Locale;
import java.util.Objects;

public record SceneLocalId(String value) {
    public SceneLocalId {
        value = Objects.requireNonNull(value, "value").trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid scene local id: " + value);
        }
    }
}
