package io.github.kxyr0.echodialogue.forge.client.ui.image.paint;

import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageBounds;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDefinition;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDrawCommand;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiTextureRegion;
import java.util.List;

/**
 * Stretches one source region across the full destination bounds.
 */
public final class UiSimpleImagePainter {
    private UiSimpleImagePainter() {
    }

    public static List<UiImageDrawCommand> commands(UiImageDefinition image, UiImageBounds bounds) {
        UiTextureRegion region = image.region();
        return List.of(new UiImageDrawCommand(image.texture(), image.mode(),
                bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                region.x(), region.y(), region.width(), region.height(),
                region.fullTextureWidth(), region.fullTextureHeight()));
    }
}
