package io.github.kxyr0.echomagic.api.identity;

import java.util.Objects;

public record MagicSchoolId(String value) {
    public MagicSchoolId {
        value = Objects.requireNonNull(value, "value");
        if (!value.contains(":")) throw new IllegalArgumentException("Invalid school id: " + value);
    }
    @Override public String toString() { return value; }
}

