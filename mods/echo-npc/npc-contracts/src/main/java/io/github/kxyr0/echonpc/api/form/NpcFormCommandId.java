package io.github.kxyr0.echonpc.api.form;

import java.util.Objects;

/**
 * Stable idempotency key for one authored Npc.SetForm command.
 */
public record NpcFormCommandId(String value) {
    public NpcFormCommandId {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty() || value.length() > 128) {
            throw new IllegalArgumentException("NpcFormCommandId must be non-empty and at most 128 characters.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
