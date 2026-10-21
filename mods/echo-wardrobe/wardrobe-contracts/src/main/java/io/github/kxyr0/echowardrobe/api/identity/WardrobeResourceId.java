package io.github.kxyr0.echowardrobe.api.identity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Shared validation for logical resource IDs without depending on Minecraft classes.
 */
public final class WardrobeResourceId {
    private static final Pattern PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private WardrobeResourceId() {
    }

    public static String require(String raw, String label) {
        String value = Objects.requireNonNull(raw, label).trim();
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(label + " must be namespace:path, got: " + raw);
        }
        return value;
    }
}
