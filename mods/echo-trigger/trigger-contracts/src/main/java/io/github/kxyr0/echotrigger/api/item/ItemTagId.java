package io.github.kxyr0.echotrigger.api.item;

import java.util.Objects;

public record ItemTagId(String value) {
    public ItemTagId {
        value = Objects.requireNonNull(value, "value").trim();
        if (!value.startsWith("#") || value.length() == 1) {
            throw new IllegalArgumentException("ItemTagId must start with '#'.");
        }
    }
}
