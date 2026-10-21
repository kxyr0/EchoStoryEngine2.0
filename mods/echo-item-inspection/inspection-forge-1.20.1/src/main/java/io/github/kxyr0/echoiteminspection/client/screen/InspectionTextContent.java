package io.github.kxyr0.echoiteminspection.client.screen;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Unicode wrapping is cached at screen init/resize and preserves authored paragraph breaks. */
final class InspectionTextContent {
    record Line(FormattedCharSequence text, boolean title) { }

    private InspectionTextContent() { }

    static List<Line> wrap(Font font, Component title, String description, int width) {
        var result = new ArrayList<Line>();
        paragraphs(result, font, title.copy().withStyle(ChatFormatting.BOLD), width, true);
        if (!description.isBlank()) {
            result.add(new Line(FormattedCharSequence.EMPTY, false));
            paragraphs(result, font, Component.literal(description), width, false);
        }
        return List.copyOf(result);
    }

    private static void paragraphs(List<Line> output, Font font, Component text, int width, boolean title) {
        for (String paragraph : text.getString().split("\\R", -1)) {
            Component component = Component.literal(paragraph).setStyle(text.getStyle());
            var lines = font.split(component, Math.max(1, width));
            if (lines.isEmpty()) output.add(new Line(FormattedCharSequence.EMPTY, title));
            else lines.forEach(line -> output.add(new Line(line, title)));
        }
    }
}
