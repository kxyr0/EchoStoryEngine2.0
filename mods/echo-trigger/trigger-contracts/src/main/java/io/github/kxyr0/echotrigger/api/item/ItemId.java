package io.github.kxyr0.echotrigger.api.item;

import java.util.Objects;

public record ItemId(String value) {
    public ItemId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty() || value.startsWith("#")) {
            throw new IllegalArgumentException("ItemId must be a non-tag registry id.");
        }
    }
}
