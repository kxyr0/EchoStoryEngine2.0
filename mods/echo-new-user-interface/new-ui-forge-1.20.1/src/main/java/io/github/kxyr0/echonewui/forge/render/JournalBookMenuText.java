package io.github.kxyr0.echonewui.forge.render;

import io.github.kxyr0.echonewui.core.layout.UiRectangle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class JournalBookMenuText {
    private static final int REFERENCE_BOOK_WIDTH = 784;
    private static final int LEFT_PAGE_CENTER_X = 215;
    private static final int FIRST_LINE_Y = 190;
    private static final int LINE_STEP = 56;
    private static final int TEXT_COLOR = 0xFF4B3328;
    private static final Component[] LINES = {
            Component.literal("Начать игру"),
            Component.literal("Настройки"),
            Component.literal("Выход в главное меню")
    };

    public void render(GuiGraphics graphics, UiRectangle bookRect) {
        var font = Minecraft.getInstance().font;
        float bookScale = bookRect.width() / (float) REFERENCE_BOOK_WIDTH;
        float textScale = Math.max(1.0F, Math.round(bookRect.width() / 392.0F));
        graphics.pose().pushPose();
        graphics.pose().scale(textScale, textScale, 1.0F);
        for (int index = 0; index < LINES.length; index++) {
            Component line = LINES[index];
            int x = scaledX(bookRect, bookScale, textScale, font.width(line));
            int y = scaledY(bookRect, bookScale, textScale, index);
            graphics.drawString(font, line, x, y, TEXT_COLOR, false);
        }
        graphics.pose().popPose();
    }

    private static int scaledX(UiRectangle rect, float bookScale, float textScale, int textWidth) {
        float screenX = rect.x() + LEFT_PAGE_CENTER_X * bookScale;
        return Math.round(screenX / textScale - textWidth / 2.0F);
    }

    private static int scaledY(UiRectangle rect, float bookScale, float textScale, int lineIndex) {
        float referenceY = FIRST_LINE_Y + lineIndex * LINE_STEP;
        float screenY = rect.y() + referenceY * bookScale;
        return Math.round(screenY / textScale);
    }
}
