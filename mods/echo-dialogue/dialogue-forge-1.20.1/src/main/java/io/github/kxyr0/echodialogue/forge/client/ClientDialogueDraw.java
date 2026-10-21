package io.github.kxyr0.echodialogue.forge.client;

import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import io.github.kxyr0.echouianimations.AnimationFrame;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/**
 * Low-level drawing helpers for animated dialogue overlay primitives.
 */
final class ClientDialogueDraw {
    private ClientDialogueDraw() {
    }

    static MutableComponent line(DialogueOpenMessage message, String visibleText) {
        return Component.literal("[" + message.speakerDisplayName() + "]: ")
                .withStyle(Style.EMPTY.withColor(speakerColor(message)))
                .append(Component.literal(visibleText)
                        .withStyle(Style.EMPTY.withColor(ClientDialogueOverlayStyle.current().textColor())));
    }

    static Component choiceText(int index, String text) {
        return Component.literal((index + 1) + ". ")
                .withStyle(Style.EMPTY.withColor(ClientDialogueOverlayStyle.current().choiceNumberColor()))
                .append(Component.literal(text)
                        .withStyle(Style.EMPTY.withColor(ClientDialogueOverlayStyle.current().choiceTextColor())));
    }

    static List<FormattedCharSequence> promptLines(
            Font font,
            DialogueOpenMessage message,
            int textWidth,
            String visibleText
    ) {
        if (visibleText.isBlank()) {
            return List.of();
        }
        return ClientDialogueLayout.visibleLines(font, line(message, visibleText), textWidth);
    }

    static void panel(GuiGraphics graphics, ClientDialogueLayout.Panel panel, AnimationFrame frame, Runnable renderer) {
        float centerX = (panel.left() + panel.right()) / 2.0F;
        float centerY = (panel.top() + panel.bottom()) / 2.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(frame.scale(), frame.scale(), 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0);
        renderer.run();
        graphics.pose().popPose();
    }

    static void textLines(GuiGraphics graphics, Font font, ClientDialogueLayout.TextBlock block, float alpha) {
        int y = 0;
        graphics.pose().pushPose();
        graphics.pose().translate(block.left(), block.top(), 0);
        graphics.pose().scale(ClientDialogueOverlayStyle.current().textScale(),
                ClientDialogueOverlayStyle.current().textScale(), 1.0F);
        for (FormattedCharSequence line : block.lines()) {
            graphics.drawString(font, line, 0, y, color(block.color(), alpha),
                    ClientDialogueOverlayStyle.current().textShadow());
            y += font.lineHeight + ClientDialogueOverlayStyle.current().lineGap();
        }
        graphics.pose().popPose();
    }

    static String visibleText(ClientDialogueAnimationState.RenderState state) {
        int length = Math.min(state.message().text().length(), state.text().visibleCharacters());
        return state.message().text().substring(0, length);
    }

    private static int speakerColor(DialogueOpenMessage message) {
        return message.speakerNameColor()
                .map(SpeakerNameColor::rgb)
                .orElse(ClientDialogueOverlayStyle.current().speakerColor());
    }

    static int color(int color, float alpha) {
        int sourceAlpha = color >>> 24;
        if (sourceAlpha == 0) {
            sourceAlpha = 255;
        }
        int blendedAlpha = Math.round(sourceAlpha * Math.max(0.0F, Math.min(1.0F, alpha)));
        return (blendedAlpha << 24) | (color & 0x00FFFFFF);
    }
}
