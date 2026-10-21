package io.github.kxyr0.echodialogue.forge.client.ui.settings;

import java.util.Objects;

/**
 * Linear fill amount for a non-sliced foreground UI image.
 */
public record UiFillSettings(UiFillDirection direction, float amount) {
    private static final UiFillSettings FULL =
            new UiFillSettings(UiFillDirection.LEFT_TO_RIGHT, 1.0F);

    public UiFillSettings {
        Objects.requireNonNull(direction, "direction");
        if (!Float.isFinite(amount) || amount < 0.0F || amount > 1.0F) {
            throw new IllegalArgumentException("UI_IMAGE_FILL_AMOUNT_INVALID: amount must be 0.0..1.0.");
        }
    }

    public static UiFillSettings full() {
        return FULL;
    }
}
