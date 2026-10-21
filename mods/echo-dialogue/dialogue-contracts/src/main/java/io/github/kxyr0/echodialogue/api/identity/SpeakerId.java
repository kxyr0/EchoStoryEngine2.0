package io.github.kxyr0.echodialogue.api.identity;

import java.util.Objects;

/**
 * Stable speaker identity independent from Minecraft entities.
 */
public record SpeakerId(NamespacedId value) {
    public SpeakerId {
        value = Objects.requireNonNull(value, "value");
    }

    public static SpeakerId parse(String raw) {
        return new SpeakerId(new NamespacedId(raw));
    }

    @Override
    public String toString() {
        return value.value();
    }
}
