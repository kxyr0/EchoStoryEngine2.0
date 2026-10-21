package io.github.kxyr0.echodialogue.api.visual;

import io.github.kxyr0.echodialogue.api.identity.NamespacedId;
import java.util.Objects;

/**
 * Source-authored neutral visual resource identity.
 */
public record SpeakerVisualId(NamespacedId value) {
    private static final int MAX_LENGTH = 128;

    public SpeakerVisualId {
        value = Objects.requireNonNull(value, "value");
        if (value.value().length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Visual ID слишком длинный.");
        }
        if (value.path().startsWith("/") || value.path().contains("//") || value.path().contains("..")) {
            throw new IllegalArgumentException("Visual ID не должен быть URL или raw path: " + value);
        }
        if (value.path().endsWith(".png")) {
            throw new IllegalArgumentException("Visual ID не должен содержать расширение файла: " + value);
        }
    }

    public static SpeakerVisualId parse(String raw) {
        return new SpeakerVisualId(new NamespacedId(raw));
    }

    @Override
    public String toString() {
        return value.value();
    }
}
