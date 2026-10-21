package io.github.kxyr0.echomagic.api.identity;

import java.util.Objects;

public record SpellCastId(String value) {
    public SpellCastId {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("Cast id must not be blank.");
    }
    @Override public String toString() { return value; }
}

