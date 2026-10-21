package io.github.kxyr0.echonpc.api.block;

import java.util.Locale;
import java.util.Objects;

/**
 * Neutral namespaced block identifier used at the Forge boundary.
 */
public record BlockId(String value) {
    private static final int MAX_LENGTH = 128;

    public BlockId {
        value = require(value);
    }

    private static String require(String value) {
        String checked = Objects.requireNonNull(value, "BlockId").trim();
        if (checked.isEmpty() || checked.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("BlockId must be non-empty and at most 128 characters.");
        }
        int separator = checked.indexOf(':');
        if (separator < 1 || separator != checked.lastIndexOf(':') || separator == checked.length() - 1) {
            throw new IllegalArgumentException("BlockId must be namespaced as namespace:path.");
        }
        requireNamespace(checked.substring(0, separator));
        requirePath(checked.substring(separator + 1));
        return checked.toLowerCase(Locale.ROOT);
    }

    private static void requireNamespace(String value) {
        if (!value.matches("[a-z0-9_.-]+")) {
            throw new IllegalArgumentException("BlockId namespace contains invalid characters.");
        }
    }

    private static void requirePath(String value) {
        if (!value.matches("[a-z0-9_./-]+") || value.contains("//")
                || value.startsWith("/") || value.endsWith("/")) {
            throw new IllegalArgumentException("BlockId path contains invalid characters.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
