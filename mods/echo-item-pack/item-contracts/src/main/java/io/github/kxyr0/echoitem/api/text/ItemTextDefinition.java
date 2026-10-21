package io.github.kxyr0.echoitem.api.text;

import java.util.List;
import java.util.Objects;

/**
 * Authored item presentation text.
 */
public record ItemTextDefinition(String name, String description, List<String> tooltip) {
    public ItemTextDefinition {
        name = requireText(name, "name");
        description = Objects.requireNonNull(description, "description");
        tooltip = List.copyOf(Objects.requireNonNull(tooltip, "tooltip"));
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return value;
    }
}
