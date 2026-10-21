package io.github.kxyr0.echonpc.core.actor;

import java.util.Objects;
import java.util.UUID;

/**
 * Opaque technical ID for one physical entity incarnation.
 */
public record EntityBindingId(String value) {
    public EntityBindingId {
        value = Objects.requireNonNull(value, "value").trim().toLowerCase(java.util.Locale.ROOT);
        if (!value.matches("[a-f0-9]{32}|[a-f0-9-]{36}")) {
            throw new IllegalArgumentException("EntityBindingId must be an opaque UUID token.");
        }
    }

    public static EntityBindingId generate() {
        return new EntityBindingId(UUID.randomUUID().toString());
    }

    public String shortValue() {
        return value.length() <= 12 ? value : value.substring(0, 12);
    }
}
