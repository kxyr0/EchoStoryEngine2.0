package io.github.kxyr0.echoanimation.api.id;

import java.util.Objects;

/**
 * Local normalized face control identity.
 */
public record FaceControlId(String value) {
    public FaceControlId {
        value = Objects.requireNonNull(value, "value");
        requireLocal(value);
    }

    public static FaceControlId parse(String value) {
        return new FaceControlId(value);
    }

    private static void requireLocal(String raw) {
        if (raw.length() > 64 || !raw.matches("[a-z][a-z0-9_/-]*")) {
            throw new IllegalArgumentException("FaceControlId должен быть bounded local id: " + raw);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

