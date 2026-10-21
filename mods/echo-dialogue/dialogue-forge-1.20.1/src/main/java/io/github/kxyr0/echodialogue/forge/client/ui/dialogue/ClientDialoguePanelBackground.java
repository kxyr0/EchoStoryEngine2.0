package io.github.kxyr0.echodialogue.forge.client.ui.dialogue;

import io.github.kxyr0.echodialogue.forge.client.config.DialogueOverlayStyleConfig;
import io.github.kxyr0.echodialogue.forge.client.config.DialoguePanelBackgroundConfig;
import io.github.kxyr0.echodialogue.forge.client.config.DialoguePanelBackgroundConfigSource;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageBounds;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDefinition;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageRenderer;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiTextureRegion;
import io.github.kxyr0.echodialogue.forge.client.ui.nineslice.UiNineSliceBounds;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiPixelSnap;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiTextureSampling;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Dialogue-panel background boundary between overlay layout and configured image rendering.
 */
public final class ClientDialoguePanelBackground {
    private static final UiImageRenderer IMAGE_RENDERER = new UiImageRenderer();
    private static final DialoguePanelBackgroundConfigSource SOURCE = new DialoguePanelBackgroundConfigSource();

    private ClientDialoguePanelBackground() {
    }

    public static void refresh() {
        SOURCE.refresh();
    }

    public static DialoguePanelBackgroundConfig currentConfig() {
        return SOURCE.current();
    }

    public static void render(
            GuiGraphics graphics,
            UiNineSliceBounds bounds,
            DialogueOverlayStyleConfig style,
            float alpha
    ) {
        DialoguePanelBackgroundConfig background = SOURCE.current();
        if (background.enabled()) {
            IMAGE_RENDERER.render(graphics, definition(background), imageBounds(bounds), alpha);
            return;
        }
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(),
                color(style.backgroundColor(), alpha));
    }

    public static UiImageDefinition definition(DialoguePanelBackgroundConfig config) {
        return new UiImageDefinition(location(config.texture()),
                UiTextureRegion.full(config.sourceWidth(), config.sourceHeight()),
                config.mode(), config.borders(), config.tileSettings(), config.fillSettings(),
                UiTextureSampling.PIXEL, UiPixelSnap.ROUND, 0);
    }

    private static UiImageBounds imageBounds(UiNineSliceBounds bounds) {
        return new UiImageBounds(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private static ResourceLocation location(String value) {
        int delimiter = value.indexOf(':');
        return ResourceLocation.fromNamespaceAndPath(
                value.substring(0, delimiter), value.substring(delimiter + 1));
    }

    private static int color(int color, float alpha) {
        int sourceAlpha = color >>> 24;
        if (sourceAlpha == 0) {
            sourceAlpha = 255;
        }
        int blendedAlpha = Math.round(sourceAlpha * Math.max(0.0F, Math.min(1.0F, alpha)));
        return (blendedAlpha << 24) | (color & 0x00FFFFFF);
    }
}
