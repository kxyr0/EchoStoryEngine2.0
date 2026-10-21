package io.github.kxyr0.echoanimation.api.id;

import java.util.Objects;

/**
 * Local face layer identity.
 */
public record FaceLayerId(String value) {
    public FaceLayerId {
        value = Objects.requireNonNull(value, "value");
        requireLocal(value);
    }

    public static FaceLayerId parse(String value) {
        return new FaceLayerId(value);
    }

    private static void requireLocal(String raw) {
        if (raw.length() > 64 || !raw.matches("[a-z][a-z0-9_/-]*")) {
            throw new IllegalArgumentException("FaceLayerId должен быть bounded local id: " + raw);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

