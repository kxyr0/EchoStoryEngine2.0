package io.github.kxyr0.echoanimation.api.graph;

import java.util.Locale;
import java.util.Objects;

public final class GraphIdSyntax {
    private static final int MAX_LENGTH = 128;

    private GraphIdSyntax() {
    }

    public static String requireNamespaced(String raw, String label) {
        String checked = require(raw, label);
        int separator = checked.indexOf(':');
        if (separator < 1 || separator != checked.lastIndexOf(':') || separator == checked.length() - 1) {
            throw new IllegalArgumentException(label + " must be namespaced as namespace:path.");
        }
        requireNamespace(checked.substring(0, separator), label);
        requirePathSyntax(checked.substring(separator + 1), label);
        return checked;
    }

    public static String requirePath(String raw, String label) {
        String checked = require(raw, label);
        requirePathSyntax(checked, label);
        return checked;
    }

    private static String require(String raw, String label) {
        String checked = Objects.requireNonNull(raw, label).trim().toLowerCase(Locale.ROOT);
        if (checked.isEmpty() || checked.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(label + " must be non-empty and at most 128 characters.");
        }
        return checked;
    }

    private static void requireNamespace(String value, String label) {
        if (!value.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException(label + " namespace contains invalid characters.");
        }
    }

    private static void requirePathSyntax(String value, String label) {
        if (!value.matches("[a-z0-9_./-]+") || value.contains("//") || value.startsWith("/") || value.endsWith("/")) {
            throw new IllegalArgumentException(label + " path contains invalid characters.");
        }
    }
}
