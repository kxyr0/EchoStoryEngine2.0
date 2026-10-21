package io.github.kxyr0.echodialogue.forge.client.ui.dialogue;

import io.github.kxyr0.echodialogue.forge.client.config.DialogueOverlayStyleConfig;
import io.github.kxyr0.echodialogue.forge.client.config.DialoguePanelBackgroundConfig;
import io.github.kxyr0.echodialogue.forge.client.config.DialoguePanelBackgroundConfigSource;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageBounds;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageRenderer;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Choice-row background boundary with the same image modes as the dialogue panel.
 */
public final class ClientDialogueChoiceBackground {
    private static final UiImageRenderer IMAGE_RENDERER = new UiImageRenderer();
    private static final DialoguePanelBackgroundConfigSource SOURCE =
            new DialoguePanelBackgroundConfigSource("DialogueChoiceBackgroundOverlay");

    private ClientDialogueChoiceBackground() {
    }

    public static void refresh() {
        SOURCE.refresh();
    }

    public static DialoguePanelBackgroundConfig currentConfig() {
        return SOURCE.current();
    }

    public static void render(
            GuiGraphics graphics,
            UiImageBounds bounds,
            DialogueOverlayStyleConfig style,
            float alpha
    ) {
        DialoguePanelBackgroundConfig background = SOURCE.current();
        if (background.enabled()) {
            IMAGE_RENDERER.render(graphics, ClientDialoguePanelBackground.definition(background), bounds, alpha);
            return;
        }
        graphics.fill(bounds.x(), bounds.y(), bounds.right(), bounds.bottom(),
                color(style.choiceBackgroundColor(), alpha));
        graphics.renderOutline(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                color(style.choiceBorderColor(), alpha));
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
