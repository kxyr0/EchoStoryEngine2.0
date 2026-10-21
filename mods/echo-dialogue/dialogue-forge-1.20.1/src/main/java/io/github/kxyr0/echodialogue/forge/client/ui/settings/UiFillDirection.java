package io.github.kxyr0.echodialogue.forge.client.ui.settings;

import java.util.Locale;
import java.util.Objects;

/**
 * Linear direction for a filled UI image.
 */
public enum UiFillDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT,
    TOP_TO_BOTTOM,
    BOTTOM_TO_TOP;

    public static UiFillDirection parse(String value) {
        String normalized = Objects.requireNonNull(value, "direction").trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        return UiFillDirection.valueOf(normalized);
    }
}
