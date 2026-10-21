package io.github.kxyr0.echodialogue.forge.client.ui.image;

import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiFillSettings;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiPixelSnap;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiTextureSampling;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiTileSettings;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * Immutable texture, source region, and render-mode definition for UI images.
 */
public record UiImageDefinition(
        ResourceLocation texture,
        UiTextureRegion region,
        UiImageRenderMode mode,
        UiSliceBorders borders,
        UiTileSettings tileSettings,
        UiFillSettings fillSettings,
        UiTextureSampling sampling,
        UiPixelSnap pixelSnap,
        int revision
) {
    public UiImageDefinition {
        Objects.requireNonNull(texture, "texture");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(mode, "mode");
        borders = Objects.requireNonNullElse(borders, UiSliceBorders.none());
        tileSettings = Objects.requireNonNullElse(tileSettings, UiTileSettings.defaults());
        fillSettings = Objects.requireNonNullElse(fillSettings, UiFillSettings.full());
        sampling = Objects.requireNonNullElse(sampling, UiTextureSampling.PIXEL);
        pixelSnap = Objects.requireNonNullElse(pixelSnap, UiPixelSnap.ROUND);
        if (mode == UiImageRenderMode.SLICED || mode == UiImageRenderMode.TILED) {
            borders.validateInside(region.width(), region.height());
        }
    }

    public static UiImageDefinition simple(ResourceLocation texture, int width, int height) {
        return image(texture, UiImageRenderMode.SIMPLE, width, height, UiSliceBorders.none());
    }

    public static UiImageDefinition sliced(
            ResourceLocation texture,
            int width,
            int height,
            UiSliceBorders borders
    ) {
        return image(texture, UiImageRenderMode.SLICED, width, height, borders);
    }

    public static UiImageDefinition image(
            ResourceLocation texture,
            UiImageRenderMode mode,
            int width,
            int height,
            UiSliceBorders borders
    ) {
        return new UiImageDefinition(texture, UiTextureRegion.full(width, height), mode, borders,
                UiTileSettings.defaults(), UiFillSettings.full(), UiTextureSampling.PIXEL, UiPixelSnap.ROUND, 0);
    }

    public UiImageDefinition withMode(UiImageRenderMode nextMode) {
        return new UiImageDefinition(texture, region, nextMode, borders, tileSettings, fillSettings,
                sampling, pixelSnap, revision + 1);
    }
}
