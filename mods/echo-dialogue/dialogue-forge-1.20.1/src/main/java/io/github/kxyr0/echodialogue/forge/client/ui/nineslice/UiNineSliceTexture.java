package io.github.kxyr0.echodialogue.forge.client.ui.nineslice;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Full source texture definition for a reusable 9-slice UI panel.
 */
public record UiNineSliceTexture(
        ResourceLocation location,
        int sourceWidth,
        int sourceHeight,
        UiNineSliceBorders borders
) {
    public UiNineSliceTexture {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(borders, "borders");
        requirePositive(sourceWidth, "sourceWidth");
        requirePositive(sourceHeight, "sourceHeight");
        borders.validateInside(sourceWidth, sourceHeight);
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException("9-slice texture " + name + " must be positive.");
        }
    }
}
