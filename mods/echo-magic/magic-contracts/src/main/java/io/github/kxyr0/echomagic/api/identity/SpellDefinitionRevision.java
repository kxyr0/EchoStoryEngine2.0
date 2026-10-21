package io.github.kxyr0.echomagic.api.identity;

import java.util.Objects;

public record SpellDefinitionRevision(String value) {
    public SpellDefinitionRevision {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("Definition revision must not be blank.");
    }

    public static SpellDefinitionRevision initial() { return new SpellDefinitionRevision("initial"); }
}

