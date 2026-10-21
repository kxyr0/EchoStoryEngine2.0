package io.github.kxyr0.echoanimationeditor.forge.client.screen.ui;

import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorEditorMode;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorTool;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;
import io.github.kxyr0.echoanimationeditor.forge.client.AnimatorForgeSession;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class AnimatorToolbarRenderer {
    private final AnimatorUiHitboxes hitboxes;

    public AnimatorToolbarRenderer(AnimatorUiHitboxes hitboxes) {
        this.hitboxes = hitboxes;
    }

    public void render(GuiGraphics graphics, Font font, AnimatorForgeSession session, AnimatorPanelRect gui) {
        DrawContext context = new DrawContext(graphics, font);
        int y = gui.top() + Math.max(5, (gui.height() - 18) / 2);
        int x = gui.left() + 10;
        graphics.drawString(font, "Echo Animator", x, y + 5, AnimatorUiTheme.TEXT, false);
        x += 118;
        x = toolbarButton(context, ButtonSpec.mode(x, y, "Body", session.state().mode() == AnimatorEditorMode.BODY,
                AnimatorEditorMode.BODY));
        x = toolbarButton(context, ButtonSpec.mode(x, y, "Face", session.state().mode() == AnimatorEditorMode.FACE,
                AnimatorEditorMode.FACE));
        x += 8;
        x = toolbarButton(context, new ButtonSpec(new AnimatorPanelRect(x, y, 34, 18), "Move",
                session.state().tool() == AnimatorTool.TRANSLATE, AnimatorUiHitKind.TOOL,
                AnimatorTool.TRANSLATE.name()));
        x = toolbarButton(context, new ButtonSpec(new AnimatorPanelRect(x, y, 34, 18), "Rot",
                session.state().tool() == AnimatorTool.ROTATE, AnimatorUiHitKind.TOOL,
                AnimatorTool.ROTATE.name()));
        toolbarButton(context, new ButtonSpec(new AnimatorPanelRect(x, y, 42, 18), "Scale",
                session.state().tool() == AnimatorTool.SCALE, AnimatorUiHitKind.TOOL,
                AnimatorTool.SCALE.name()));
        int right = gui.right() - 10;
        right = actionButton(context, right, ButtonSpec.action(y, 48, "Reload", AnimatorUiAction.HOT_RELOAD));
        right = actionButton(context, right, ButtonSpec.action(y, 46, "Clear", AnimatorUiAction.CLEAR_PREVIEW));
        right = actionButton(context, right, ButtonSpec.action(y, 54, "Preview", AnimatorUiAction.PREVIEW));
        right = actionButton(context, right, ButtonSpec.action(y, 42, "Redo", AnimatorUiAction.REDO));
        right = actionButton(context, right, ButtonSpec.action(y, 42, "Undo", AnimatorUiAction.UNDO));
        actionButton(context, right, ButtonSpec.action(y, 42, "Save", AnimatorUiAction.SAVE));
    }

    int toolbarButton(DrawContext context, ButtonSpec spec) {
        button(context, spec);
        return spec.rect().right() + 4;
    }

    int actionButton(DrawContext context, int right, ButtonSpec spec) {
        AnimatorPanelRect rect = new AnimatorPanelRect(right - spec.rect().width(), spec.rect().top(),
                spec.rect().width(), spec.rect().height());
        button(context, spec.at(rect));
        return rect.left() - 4;
    }

    void button(DrawContext context, ButtonSpec spec) {
        AnimatorPanelRect rect = spec.rect();
        GuiGraphics graphics = context.graphics();
        Font font = context.font();
        graphics.fill(rect.left(), rect.top(), rect.right(), rect.bottom(),
                spec.active() ? AnimatorUiTheme.BUTTON_ACTIVE : AnimatorUiTheme.BUTTON);
        graphics.renderOutline(rect.left(), rect.top(), rect.width(), rect.height(),
                spec.active() ? AnimatorUiTheme.GREEN : AnimatorUiTheme.LINE_SOFT);
        String fitted = AnimatorUiText.fit(font, spec.label(), rect.width() - 8);
        int textX = rect.left() + Math.max(4, (rect.width() - font.width(fitted)) / 2);
        graphics.drawString(font, fitted, textX, rect.top() + (rect.height() - 8) / 2,
                spec.active() ? AnimatorUiTheme.TEXT : AnimatorUiTheme.MUTED, false);
        hitboxes.add(rect, spec.kind(), spec.value());
    }

    record DrawContext(GuiGraphics graphics, Font font) {
    }

    record ButtonSpec(AnimatorPanelRect rect, String label, boolean active, AnimatorUiHitKind kind, String value) {
        static ButtonSpec mode(int x, int y, String label, boolean active, AnimatorEditorMode mode) {
            return new ButtonSpec(new AnimatorPanelRect(x, y, 46, 18), label, active,
                    AnimatorUiHitKind.MODE, mode.name());
        }

        static ButtonSpec action(int y, int width, String label, AnimatorUiAction action) {
            return new ButtonSpec(new AnimatorPanelRect(0, y, width, 18), label, false,
                    AnimatorUiHitKind.ACTION, action.name());
        }

        ButtonSpec at(AnimatorPanelRect nextRect) {
            return new ButtonSpec(nextRect, label, active, kind, value);
        }
    }
}
