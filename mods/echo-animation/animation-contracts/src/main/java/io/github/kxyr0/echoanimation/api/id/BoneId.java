package io.github.kxyr0.echoanimation.api.id;

import java.util.Objects;

/**
 * Local face rig bone identity.
 */
public record BoneId(String value) {
    public BoneId {
        value = Objects.requireNonNull(value, "value");
        requireLocal(value);
    }

    public static BoneId parse(String value) {
        return new BoneId(value);
    }

    private static void requireLocal(String raw) {
        if (raw.length() > 64 || !raw.matches("[a-z][a-z0-9_/-]*")) {
            throw new IllegalArgumentException("BoneId должен быть bounded local id: " + raw);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

