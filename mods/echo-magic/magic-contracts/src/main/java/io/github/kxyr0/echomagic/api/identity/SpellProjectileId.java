package io.github.kxyr0.echomagic.api.identity;

import java.util.Objects;

public record SpellProjectileId(String value) {
    public SpellProjectileId {
        value = Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("Projectile id must not be blank.");
    }
    @Override public String toString() { return value; }
}

