package io.github.kxyr0.echomagic.api.visual;

import java.util.Objects;

public record MagicParticleStyleId(String value) {
    public MagicParticleStyleId {
        value = Objects.requireNonNull(value, "value");
        if (!value.contains(":")) throw new IllegalArgumentException("Invalid particle style id: " + value);
    }
    @Override public String toString() { return value; }
}

