package io.github.kxyr0.echoanimationeditor.forge.client.screen.ui;

import net.minecraft.client.gui.Font;

final class AnimatorUiText {
    private AnimatorUiText() {
    }

    static String fit(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String suffix = "...";
        int allowed = Math.max(0, width - font.width(suffix));
        StringBuilder builder = new StringBuilder(text);
        while (!builder.isEmpty() && font.width(builder.toString()) > allowed) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder + suffix;
    }
}
