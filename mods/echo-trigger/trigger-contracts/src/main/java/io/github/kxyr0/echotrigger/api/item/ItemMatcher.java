package io.github.kxyr0.echotrigger.api.item;

import java.util.Objects;

public record ItemMatcher(String value, boolean tag) {
    public ItemMatcher {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty() || tag != value.startsWith("#")) {
            throw new IllegalArgumentException("ItemMatcher tag flag must match the value prefix.");
        }
    }

    public static ItemMatcher parse(String value) {
        return new ItemMatcher(value, Objects.requireNonNull(value, "value").trim().startsWith("#"));
    }
}
