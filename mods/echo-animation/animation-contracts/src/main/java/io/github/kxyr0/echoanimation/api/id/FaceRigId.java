package io.github.kxyr0.echoanimation.api.id;

import java.util.Objects;

/**
 * Stable authored namespaced face rig identity.
 */
public record FaceRigId(String value) {
    public FaceRigId {
        value = Objects.requireNonNull(value, "value");
        requireNamespaced(value);
    }

    public static FaceRigId parse(String value) {
        return new FaceRigId(value);
    }

    private static void requireNamespaced(String raw) {
        int colon = raw.indexOf(':');
        if (colon <= 0 || colon != raw.lastIndexOf(':') || colon == raw.length() - 1) {
            throw new IllegalArgumentException("FaceRigId должен иметь вид namespace:path.");
        }
        if (!raw.substring(0, colon).matches("[a-z0-9_.-]+")
                || !raw.substring(colon + 1).matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("FaceRigId содержит недопустимые символы: " + raw);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

