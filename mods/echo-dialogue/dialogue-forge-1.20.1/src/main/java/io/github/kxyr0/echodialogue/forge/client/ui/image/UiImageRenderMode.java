package io.github.kxyr0.echodialogue.forge.client.ui.image;

import java.util.Locale;
import java.util.Objects;

/**
 * Typed UI image rendering modes.
 */
public enum UiImageRenderMode {
    SIMPLE,
    SLICED,
    TILED,
    FILLED;

    public static UiImageRenderMode parse(String value) {
        String normalized = Objects.requireNonNull(value, "mode").trim()
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
        if ("NINE_SLICE".equals(normalized) || "9_SLICE".equals(normalized)) {
            return SLICED;
        }
        return UiImageRenderMode.valueOf(normalized);
    }
}
