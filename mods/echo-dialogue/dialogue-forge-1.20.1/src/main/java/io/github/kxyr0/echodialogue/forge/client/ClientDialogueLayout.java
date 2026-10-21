package io.github.kxyr0.echodialogue.forge.client;

import io.github.kxyr0.echodialogue.forge.client.ui.nineslice.UiNineSliceBounds;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Size and position calculations for the compact dialogue overlay.
 */
final class ClientDialogueLayout {
    private ClientDialogueLayout() {
    }

    static int availableTextWidth(int screenWidth) {
        return availableTextWidth(screenWidth, false);
    }

    static int availableTextWidth(int screenWidth, boolean visual) {
        int visualWidth = visual
                ? ClientDialogueOverlayStyle.current().visualSize() + ClientDialogueOverlayStyle.current().visualGap()
                : 0;
        int maxByScreen = Math.max(ClientDialogueOverlayStyle.current().minTextWidth(),
                unscaled(screenWidth - ClientDialogueOverlayStyle.current().screenMarginX() * 2)
                        - ClientDialogueOverlayStyle.current().paddingX() * 2 - visualWidth);
        return Math.min(ClientDialogueOverlayStyle.current().maxTextWidth(), maxByScreen);
    }

    static boolean canShowVisual(int screenWidth) {
        int width = ClientDialogueOverlayStyle.current().minTextWidth()
                + ClientDialogueOverlayStyle.current().visualSize()
                + ClientDialogueOverlayStyle.current().visualGap()
                + ClientDialogueOverlayStyle.current().paddingX() * 2;
        return scaled(width) <= screenWidth - ClientDialogueOverlayStyle.current().screenMarginX() * 2;
    }

    static List<FormattedCharSequence> visibleLines(Font font, Component text, int textWidth) {
        List<FormattedCharSequence> wrapped = font.split(text, textWidth);
        return wrapped.isEmpty() ? List.of(Component.literal("").getVisualOrderText()) : wrapped;
    }

    static int contentWidth(Font font, List<FormattedCharSequence> lines) {
        int widest = ClientDialogueOverlayStyle.current().minTextWidth();
        for (FormattedCharSequence line : lines) {
            widest = Math.max(widest, font.width(line));
        }
        return widest;
    }

    static int contentHeight(Font font, int lineCount) {
        return lineCount * font.lineHeight
                + Math.max(0, lineCount - 1) * ClientDialogueOverlayStyle.current().lineGap();
    }

    static int choiceTextWidth(int contentWidth) {
        return Math.max(ClientDialogueOverlayStyle.current().minTextWidth(),
                contentWidth - ClientDialogueOverlayStyle.current().choicePaddingX() * 2);
    }

    static int choiceContentWidth(List<ChoiceRow> rows) {
        return rows.stream().mapToInt(ChoiceRow::width)
                .max().orElse(ClientDialogueOverlayStyle.current().minTextWidth());
    }

    static int choicePresentationHeight(
            Font font,
            List<FormattedCharSequence> promptLines,
            List<ChoiceRow> rows,
            boolean visual
    ) {
        int choiceHeight = choiceContentHeight(rows);
        int promptHeight = contentHeight(font, promptLines.size());
        int promptRowHeight = visual
                ? Math.max(promptHeight, ClientDialogueOverlayStyle.current().visualSize()) : promptHeight;
        if (rows.isEmpty()) {
            return promptRowHeight;
        }
        if (promptLines.isEmpty()) {
            return choiceHeight;
        }
        return promptRowHeight + ClientDialogueOverlayStyle.current().choiceSectionGap() + choiceHeight;
    }

    static int panelContentWidth(int textContentWidth, boolean visual) {
        if (!visual) {
            return textContentWidth;
        }
        return textContentWidth
                + ClientDialogueOverlayStyle.current().visualSize()
                + ClientDialogueOverlayStyle.current().visualGap();
    }

    static int panelContentHeight(int textContentHeight, boolean visual) {
        if (!visual) {
            return textContentHeight;
        }
        return Math.max(textContentHeight, ClientDialogueOverlayStyle.current().visualSize());
    }

    static Panel panel(int screenWidth, int screenHeight, int contentWidth, int contentHeight) {
        int panelWidth = scaled(contentWidth + ClientDialogueOverlayStyle.current().paddingX() * 2);
        int panelHeight = scaled(contentHeight + ClientDialogueOverlayStyle.current().paddingY() * 2);
        int left = clamp(anchoredLeft(screenWidth, panelWidth), ClientDialogueOverlayStyle.current().screenMarginX(),
                screenWidth - panelWidth - ClientDialogueOverlayStyle.current().screenMarginX());
        int top = clamp(anchoredTop(screenHeight, panelHeight), ClientDialogueOverlayStyle.current().screenMarginY(),
                screenHeight - panelHeight - ClientDialogueOverlayStyle.current().screenMarginY());
        return new Panel(left, top, left + panelWidth, top + panelHeight);
    }

    static int textLeft(Panel panel) {
        return panel.left() + scaled(ClientDialogueOverlayStyle.current().paddingX());
    }

    static int textTop(Panel panel) {
        return panel.top() + scaled(ClientDialogueOverlayStyle.current().paddingY());
    }

    static int blockTop(Panel panel, int rowHeight, int blockHeight) {
        return blockTop(panel, rowHeight, blockHeight, 0);
    }

    static int blockTop(Panel panel, int rowHeight, int blockHeight, int offsetY) {
        int offset = Math.max(0, rowHeight - blockHeight) / 2 + offsetY;
        return textTop(panel) + scaled(offset);
    }

    static int contentLeft(Panel panel, boolean visual) {
        int left = textLeft(panel);
        if (!visual) {
            return left;
        }
        return left + scaled(ClientDialogueOverlayStyle.current().visualSize()
                + ClientDialogueOverlayStyle.current().visualGap());
    }

    static int visualLeft(Panel panel) {
        return textLeft(panel) + scaled(ClientDialogueOverlayStyle.current().visualOffsetX());
    }

    static int visualTop(Panel panel, int contentHeight) {
        int extra = Math.max(0, contentHeight - ClientDialogueOverlayStyle.current().visualSize()) / 2;
        return textTop(panel) + scaled(extra + ClientDialogueOverlayStyle.current().visualOffsetY());
    }

    static int scaled(int value) {
        return Math.round(value * ClientDialogueOverlayStyle.current().textScale());
    }

    private static int choiceContentHeight(List<ChoiceRow> rows) {
        int rowHeight = rows.stream().mapToInt(ChoiceRow::height).sum();
        int gaps = Math.max(0, rows.size() - 1) * ClientDialogueOverlayStyle.current().choiceGap();
        return rowHeight + gaps;
    }

    private static int unscaled(int value) {
        return Math.round(value / ClientDialogueOverlayStyle.current().textScale());
    }

    private static int anchoredLeft(int screenWidth, int panelWidth) {
        return anchorCoordinate(screenWidth, ClientDialogueOverlayStyle.current().anchorX())
                + ClientDialogueOverlayStyle.current().anchorOffsetX() - panelWidth / 2;
    }

    private static int anchoredTop(int screenHeight, int panelHeight) {
        int centeredTop = anchorCoordinate(screenHeight, ClientDialogueOverlayStyle.current().anchorY())
                + ClientDialogueOverlayStyle.current().anchorOffsetY() - panelHeight / 2;
        int crosshairY = anchorCoordinate(screenHeight, 0.5F);
        int crosshairTop = crosshairY - ClientDialogueOverlayStyle.current().crosshairPaddingY();
        int crosshairBottom = crosshairY + ClientDialogueOverlayStyle.current().crosshairPaddingY();
        if (centeredTop <= crosshairBottom && centeredTop + panelHeight >= crosshairTop) {
            return crosshairBottom;
        }
        return centeredTop;
    }

    private static int anchorCoordinate(int size, float anchor) {
        return Math.round(size * anchor);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, Math.max(min, max)));
    }

    record Panel(int left, int top, int right, int bottom) {
        UiNineSliceBounds bounds() {
            return UiNineSliceBounds.fromEdges(left, top, right, bottom);
        }
    }

    record ChoiceRow(String choiceId, List<FormattedCharSequence> lines, int width, int height) {
    }

    record TextBlock(List<FormattedCharSequence> lines, int left, int top, int color) {
    }

    record ChoiceHitBox(String choiceId, int left, int top, int right, int bottom) {
        boolean contains(double x, double y) {
            return x >= left && x <= right && y >= top && y <= bottom;
        }
    }
}
