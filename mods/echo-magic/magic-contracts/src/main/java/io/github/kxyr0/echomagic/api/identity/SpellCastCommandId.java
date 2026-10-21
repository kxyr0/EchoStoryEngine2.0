package io.github.kxyr0.echomagic.api.identity;

import java.util.Objects;

public record SpellCastCommandId(String value) {
    public SpellCastCommandId {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("Cast command id must not be blank.");
    }
    @Override public String toString() { return value; }
}

