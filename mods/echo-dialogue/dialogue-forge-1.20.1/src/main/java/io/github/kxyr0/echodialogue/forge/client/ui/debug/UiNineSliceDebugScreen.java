package io.github.kxyr0.echodialogue.forge.client.ui.debug;

import io.github.kxyr0.echodialogue.forge.client.config.DialoguePanelBackgroundConfig;
import io.github.kxyr0.echodialogue.forge.client.ui.dialogue.ClientDialoguePanelBackground;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageBounds;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDefinition;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageRenderMode;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Client dev screen that draws every UI image mode in representative sizes.
 */
final class UiNineSliceDebugScreen extends Screen {
    private static final int LABEL_COLOR = 0xFFE6E6E6;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int MARGIN = 14;
    private static final int ROW_GAP = 68;
    private static final UiImageRenderer RENDERER = new UiImageRenderer();

    UiNineSliceDebugScreen() {
        super(Component.literal("Echo Dialogue UI image modes"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Font font = Minecraft.getInstance().font;
        graphics.drawCenteredString(font, title, width / 2, 12, TITLE_COLOR);
        DialoguePanelBackgroundConfig base = ClientDialoguePanelBackground.currentConfig();
        for (SamplePanel panel : panels()) {
            drawPanel(graphics, font, base, panel);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private List<SamplePanel> panels() {
        List<SamplePanel> result = new ArrayList<>();
        int top = 36;
        int panelWidth = Math.max(42, Math.min(104, (width - MARGIN * 2 - 102) / 3));
        int[] widths = {Math.min(56, panelWidth), panelWidth, Math.min(120, panelWidth + 16)};
        int[] heights = {28, 38, 58};
        for (int row = 0; row < UiImageRenderMode.values().length; row++) {
            int y = top + row * ROW_GAP;
            int x = MARGIN + 78;
            for (int index = 0; index < widths.length; index++) {
                result.add(new SamplePanel(UiImageRenderMode.values()[row], x, y, widths[index], heights[index]));
                x += panelWidth + 12;
            }
        }
        return result;
    }

    private static void drawPanel(
            GuiGraphics graphics,
            Font font,
            DialoguePanelBackgroundConfig base,
            SamplePanel panel
    ) {
        if (panel.x() == MARGIN + 78) {
            graphics.drawString(font, panel.mode().name(), MARGIN, panel.y() + 8, LABEL_COLOR, false);
        }
        UiImageDefinition image = ClientDialoguePanelBackground.definition(base.withMode(panel.mode()));
        RENDERER.render(graphics, image, new UiImageBounds(panel.x(), panel.y(), panel.width(), panel.height()));
    }

    private record SamplePanel(UiImageRenderMode mode, int x, int y, int width, int height) {
    }
}
