package io.github.kxyr0.echodialogue.forge.client;

import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.forge.client.config.DialogueOverlayStyleConfig;
import io.github.kxyr0.echodialogue.forge.client.ui.dialogue.ClientDialogueChoiceBackground;
import io.github.kxyr0.echodialogue.forge.client.ui.dialogue.ClientDialoguePanelBackground;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageBounds;
import io.github.kxyr0.echodialogue.forge.client.visual.ClientSpeakerVisual;
import io.github.kxyr0.echodialogue.forge.client.visual.ClientSpeakerVisualRenderer;
import io.github.kxyr0.echodialogue.forge.client.visual.ClientSpeakerVisualResolver;
import io.github.kxyr0.echodialogue.forge.network.message.DialogueOpenMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Client-only compact dialogue panel renderer and choice hit testing.
 */
final class ClientDialogueRenderer {
    private static final float THOUGHT_VISUAL_ALPHA = 0.55F;
    private static final List<ClientDialogueLayout.ChoiceHitBox> CHOICE_HITS = new ArrayList<>();
    private static final ClientSpeakerVisualResolver VISUALS = new ClientSpeakerVisualResolver();

    private ClientDialogueRenderer() {}

    static void render(GuiGraphics graphics, ClientDialogueAnimationState anim, int width, int height, long nowMs) {
        ClientDialogueOverlayStyle.refresh();
        ClientDialoguePanelBackground.refresh();
        ClientDialogueChoiceBackground.refresh();
        CHOICE_HITS.clear();
        RenderContext context = new RenderContext(graphics, anim, width, height, nowMs);
        anim.renderState(nowMs).ifPresent(state -> renderState(context, state));
    }

    private static void renderState(RenderContext context, ClientDialogueAnimationState.RenderState state) {
        if (state.message().kind() == DialoguePresentationKind.CHOICE) { renderChoices(context, state); return; }
        renderLine(context, state);
    }

    static Optional<String> choiceAt(double x, double y) {
        return CHOICE_HITS.stream().filter(hitBox -> hitBox.contains(x, y))
                .map(ClientDialogueLayout.ChoiceHitBox::choiceId).findFirst();
    }

    static void clearVisualCache() { VISUALS.clear(); }

    private static void renderLine(RenderContext context, ClientDialogueAnimationState.RenderState state) {
        Font font = Minecraft.getInstance().font;
        DialogueOpenMessage message = state.message();
        Optional<ClientSpeakerVisual> visual = resolvedVisual(message, context.width());
        int wrapLimit = ClientDialogueLayout.availableTextWidth(context.width(), visual.isPresent());
        List<FormattedCharSequence> targetLines = ClientDialogueLayout.visibleLines(
                font, ClientDialogueDraw.line(message, message.text()), wrapLimit);
        int textWidth = ClientDialogueLayout.contentWidth(font, targetLines);
        List<FormattedCharSequence> visibleLines = ClientDialogueLayout.visibleLines(
                font, ClientDialogueDraw.line(message, ClientDialogueDraw.visibleText(state)), textWidth);
        int textHeight = ClientDialogueLayout.contentHeight(font, targetLines.size());
        int contentWidth = ClientDialogueLayout.panelContentWidth(textWidth, visual.isPresent());
        int contentHeight = ClientDialogueLayout.panelContentHeight(textHeight, visual.isPresent());
        ClientDialogueAnimationState.PanelSize panelSize = context.animation().panelSize(
                contentWidth, contentHeight, context.nowMs());
        ClientDialogueLayout.Panel panel = ClientDialogueLayout.panel(context.width(), context.height(),
                panelSize.width(), panelSize.height());
        ClientDialogueDraw.panel(context.graphics(), panel, state.panel(), () -> {
            ClientDialoguePanelBackground.render(context.graphics(), panel.bounds(),
                    style(), state.panel().alpha());
            visual.ifPresent(value -> renderVisual(context.graphics(), value, panel, contentHeight,
                    visualAlpha(state)));
            ClientDialogueDraw.textLines(context.graphics(), font,
                new ClientDialogueLayout.TextBlock(visibleLines,
                        ClientDialogueLayout.contentLeft(panel, visual.isPresent()),
                        ClientDialogueLayout.blockTop(panel, contentHeight, textHeight,
                                visualTextOffsetY(visual.isPresent())),
                        style().textColor()),
                    textAlpha(state));
        });
    }

    private static void renderChoices(RenderContext context, ClientDialogueAnimationState.RenderState state) {
        Font font = Minecraft.getInstance().font;
        DialogueOpenMessage message = state.message();
        Optional<ClientSpeakerVisual> visual = resolvedVisual(message, context.width())
                .filter(value -> !message.text().isBlank());
        int wrapLimit = ClientDialogueLayout.availableTextWidth(context.width(), visual.isPresent());
        int choiceTextWidth = ClientDialogueLayout.choiceTextWidth(wrapLimit);
        boolean showChoices = state.closing() || state.text().finished();
        List<FormattedCharSequence> targetPromptLines = ClientDialogueDraw.promptLines(
                font, message, wrapLimit, message.text());
        List<ClientDialogueLayout.ChoiceRow> allRows = choiceRows(font, message, choiceTextWidth);
        int textWidth = Math.max(ClientDialogueLayout.contentWidth(font, targetPromptLines),
                ClientDialogueLayout.choiceContentWidth(allRows));
        List<FormattedCharSequence> visiblePromptLines = ClientDialogueDraw.promptLines(
                font, message, textWidth, ClientDialogueDraw.visibleText(state));
        List<ClientDialogueLayout.ChoiceRow> visibleRows = showChoices ? allRows : List.of();
        int promptHeight = ClientDialogueLayout.contentHeight(font, targetPromptLines.size());
        int promptRowHeight = visual.isPresent() ? Math.max(promptHeight, style().visualSize()) : promptHeight;
        int textHeight = ClientDialogueLayout.choicePresentationHeight(
                font, targetPromptLines, allRows, visual.isPresent());
        int contentWidth = ClientDialogueLayout.panelContentWidth(textWidth, visual.isPresent());
        int contentHeight = ClientDialogueLayout.panelContentHeight(textHeight, visual.isPresent());
        ClientDialogueAnimationState.PanelSize panelSize = context.animation()
                .panelSize(contentWidth, contentHeight, context.nowMs());
        ClientDialogueLayout.Panel panel = ClientDialogueLayout.panel(context.width(), context.height(),
                panelSize.width(), panelSize.height());
        ChoiceFrame frame = new ChoiceFrame(visual, visiblePromptLines, visibleRows, promptHeight, promptRowHeight);
        ClientDialogueDraw.panel(context.graphics(), panel, state.panel(), () ->
                renderChoicePanelContent(context, state, panel, frame));
    }

    private static void renderChoicePanelContent(RenderContext context, ClientDialogueAnimationState.RenderState state,
            ClientDialogueLayout.Panel panel, ChoiceFrame frame) {
        Font font = Minecraft.getInstance().font;
        ClientDialoguePanelBackground.render(context.graphics(), panel.bounds(), style(),
                state.panel().alpha());
        frame.visual().ifPresent(value -> renderVisual(context.graphics(), value, panel, frame.promptRowHeight(),
                visualAlpha(state)));
        int contentLeft = ClientDialogueLayout.contentLeft(panel, frame.visual().isPresent());
        int y = ClientDialogueLayout.textTop(panel);
        if (!frame.visiblePromptLines().isEmpty()) {
            ClientDialogueDraw.textLines(context.graphics(), font,
                    new ClientDialogueLayout.TextBlock(
                            frame.visiblePromptLines(), contentLeft,
                            ClientDialogueLayout.blockTop(panel, frame.promptRowHeight(), frame.promptHeight(),
                                    visualTextOffsetY(frame.visual().isPresent())),
                            style().textColor()),
                    textAlpha(state));
            y += ClientDialogueLayout.scaled(frame.promptRowHeight() + style().choiceSectionGap());
        }
        ChoiceRenderContext choiceContext = new ChoiceRenderContext(context.graphics(), font, contentLeft,
                textAlpha(state));
        for (ClientDialogueLayout.ChoiceRow row : frame.visibleRows()) {
            renderChoiceRow(choiceContext, row, y);
            y += ClientDialogueLayout.scaled(row.height() + ClientDialogueOverlayStyle.current().choiceGap());
        }
    }

    private static List<ClientDialogueLayout.ChoiceRow> choiceRows(Font font, DialogueOpenMessage message,
            int textWidth) {
        List<ClientDialogueLayout.ChoiceRow> rows = new ArrayList<>();
        List<DialogueOpenMessage.ChoicePayload> choices = message.choices();
        for (int index = 0; index < choices.size(); index++) {
            DialogueOpenMessage.ChoicePayload choice = choices.get(index);
            Component text = ClientDialogueDraw.choiceText(index, choice.text());
            List<FormattedCharSequence> lines = ClientDialogueLayout.visibleLines(font, text, textWidth);
            int rowWidth = ClientDialogueLayout.contentWidth(font, lines) + style().choicePaddingX() * 2;
            int rowHeight = ClientDialogueLayout.contentHeight(font, lines.size()) + style().choicePaddingY() * 2;
            rows.add(new ClientDialogueLayout.ChoiceRow(choice.id(), lines, rowWidth, rowHeight));
        }
        return rows;
    }

    private static void renderChoiceRow(ChoiceRenderContext context, ClientDialogueLayout.ChoiceRow row, int y) {
        int left = context.contentLeft();
        int width = ClientDialogueLayout.scaled(row.width());
        int height = ClientDialogueLayout.scaled(row.height());
        int right = left + width;
        ClientDialogueChoiceBackground.render(context.graphics(), new UiImageBounds(left, y, width, height),
                style(), context.alpha());
        CHOICE_HITS.add(new ClientDialogueLayout.ChoiceHitBox(row.choiceId(), left, y, right, y + height));
        ClientDialogueDraw.textLines(context.graphics(), context.font(), new ClientDialogueLayout.TextBlock(row.lines(),
                left + ClientDialogueLayout.scaled(style().choicePaddingX()),
                y + ClientDialogueLayout.scaled(style().choicePaddingY()),
                style().choiceTextColor()), context.alpha());
    }

    private static Optional<ClientSpeakerVisual> resolvedVisual(DialogueOpenMessage message, int screenWidth) {
        if (!ClientDialogueLayout.canShowVisual(screenWidth)) { return Optional.empty(); }
        return VISUALS.resolve(message.visual());
    }

    private static void renderVisual(GuiGraphics graphics, ClientSpeakerVisual visual,
            ClientDialogueLayout.Panel panel, int contentHeight, float alpha) {
        int size = ClientDialogueLayout.scaled(style().visualSize());
        ClientSpeakerVisualRenderer.render(graphics, visual, new ClientSpeakerVisualRenderer.RenderPlacement(
                ClientDialogueLayout.visualLeft(panel), ClientDialogueLayout.visualTop(panel, contentHeight), size,
                alpha));
    }

    private static float textAlpha(ClientDialogueAnimationState.RenderState state) { return Math.min(
            state.panel().alpha(), state.textFade().alpha()); }

    static float visualAlpha(ClientDialogueAnimationState.RenderState state) { return visualAlpha(
            state.message().thought(), state.panel().alpha()); }

    static float visualAlpha(boolean thought, float panelAlpha) { return thought ? panelAlpha * THOUGHT_VISUAL_ALPHA
            : panelAlpha; }

    private static int visualTextOffsetY(boolean visual) { return visual ? style().visualTextOffsetY() : 0; }

    private static DialogueOverlayStyleConfig style() { return ClientDialogueOverlayStyle.current(); }

    private record RenderContext(GuiGraphics graphics, ClientDialogueAnimationState animation, int width, int height,
            long nowMs) {}
    private record ChoiceFrame(Optional<ClientSpeakerVisual> visual, List<FormattedCharSequence> visiblePromptLines,
            List<ClientDialogueLayout.ChoiceRow> visibleRows, int promptHeight, int promptRowHeight) {}
    private record ChoiceRenderContext(GuiGraphics graphics, Font font, int contentLeft, float alpha) {}
}
