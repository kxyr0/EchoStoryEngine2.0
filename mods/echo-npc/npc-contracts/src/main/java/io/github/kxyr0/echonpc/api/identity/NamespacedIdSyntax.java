package io.github.kxyr0.echonpc.api.identity;

import java.util.Locale;
import java.util.Objects;

public final class NamespacedIdSyntax {
    private static final int MAX_LENGTH = 128;

    private NamespacedIdSyntax() {
    }

    public static String require(String value, String label) {
        String checked = Objects.requireNonNull(value, label).trim();
        if (checked.isEmpty() || checked.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(label + " must be non-empty and at most 128 characters.");
        }
        int separator = checked.indexOf(':');
        if (separator < 1 || separator != checked.lastIndexOf(':') || separator == checked.length() - 1) {
            throw new IllegalArgumentException(label + " must be namespaced as namespace:path.");
        }
        requireNamespace(checked.substring(0, separator), label);
        requirePath(checked.substring(separator + 1), label);
        return checked.toLowerCase(Locale.ROOT);
    }

    private static void requireNamespace(String value, String label) {
        if (!value.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException(label + " namespace contains invalid characters.");
        }
    }

    private static void requirePath(String value, String label) {
        if (!value.matches("[a-z0-9_./-]+") || value.contains("//") || value.startsWith("/") || value.endsWith("/")) {
            throw new IllegalArgumentException(label + " path contains invalid characters.");
        }
    }
}
