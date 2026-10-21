package io.github.kxyr0.echomagic.api.identity;

import java.util.Objects;

public record MagicDamageTypeId(String value) {
    public MagicDamageTypeId {
        value = Objects.requireNonNull(value, "value");
        if (!value.contains(":")) throw new IllegalArgumentException("Invalid damage type id: " + value);
    }
    @Override public String toString() { return value; }
}

