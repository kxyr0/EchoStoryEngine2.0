package io.github.kxyr0.echodialogue.forge.client.ui.image.paint;

import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageBounds;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDefinition;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDrawCommand;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiTextureRegion;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiFillDirection;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiFillSettings;
import java.util.List;

/**
 * Clips simple image geometry by a linear fill amount.
 */
public final class UiFilledImagePainter {
    private UiFilledImagePainter() {
    }

    public static List<UiImageDrawCommand> commands(UiImageDefinition image, UiImageBounds bounds) {
        UiFillSettings fill = image.fillSettings();
        if (fill.amount() == 0.0F) {
            return List.of();
        }
        UiTextureRegion region = image.region();
        Clip clip = clip(fill.direction(), fill.amount(), bounds, region);
        return List.of(new UiImageDrawCommand(image.texture(), image.mode(),
                clip.x(), clip.y(), clip.width(), clip.height(),
                clip.sourceX(), clip.sourceY(), clip.sourceWidth(), clip.sourceHeight(),
                region.fullTextureWidth(), region.fullTextureHeight()));
    }

    private static Clip clip(
            UiFillDirection direction,
            float amount,
            UiImageBounds bounds,
            UiTextureRegion region
    ) {
        int targetWidth = Math.max(1, Math.round(bounds.width() * amount));
        int targetHeight = Math.max(1, Math.round(bounds.height() * amount));
        int sourceWidth = Math.max(1, Math.round(region.width() * amount));
        int sourceHeight = Math.max(1, Math.round(region.height() * amount));
        return switch (direction) {
            case LEFT_TO_RIGHT -> new Clip(bounds.x(), bounds.y(), targetWidth, bounds.height(),
                    region.x(), region.y(), sourceWidth, region.height());
            case RIGHT_TO_LEFT -> new Clip(bounds.right() - targetWidth, bounds.y(), targetWidth, bounds.height(),
                    region.right() - sourceWidth, region.y(), sourceWidth, region.height());
            case TOP_TO_BOTTOM -> new Clip(bounds.x(), bounds.y(), bounds.width(), targetHeight,
                    region.x(), region.y(), region.width(), sourceHeight);
            case BOTTOM_TO_TOP -> new Clip(bounds.x(), bounds.bottom() - targetHeight, bounds.width(), targetHeight,
                    region.x(), region.bottom() - sourceHeight, region.width(), sourceHeight);
        };
    }

    private record Clip(
            int x,
            int y,
            int width,
            int height,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
    }
}
