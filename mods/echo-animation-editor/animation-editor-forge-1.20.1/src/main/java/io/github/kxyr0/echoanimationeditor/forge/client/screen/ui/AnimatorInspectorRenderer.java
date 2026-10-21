package io.github.kxyr0.echoanimationeditor.forge.client.screen.ui;

import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;
import io.github.kxyr0.echoanimationeditor.forge.client.AnimatorDiscoveryState;
import io.github.kxyr0.echoanimationeditor.forge.client.AnimatorForgeSession;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class AnimatorInspectorRenderer {
    private static final int ROW_HEIGHT = 14;
    private final AnimatorToolbarRenderer buttons;
    private final AnimatorUiHitboxes hitboxes;

    public AnimatorInspectorRenderer(AnimatorToolbarRenderer buttons, AnimatorUiHitboxes hitboxes) {
        this.buttons = buttons;
        this.hitboxes = hitboxes;
    }

    public void render(GuiGraphics graphics, Font font, AnimatorForgeSession session, AnimatorPanelRect gui) {
        Context context = new Context(graphics, font, session, gui.left() + 10, gui.width() - 20);
        int y = gui.top() + 10;
        graphics.drawString(font, "Inspector", context.x(), y, AnimatorUiTheme.TEXT, false);
        y = renderActionGrid(context, y + 18) + 8;
        y = renderSelection(context, y) + 8;
        AnimatorDiscoveryState discovery = session.discovery();
        int bottom = gui.bottom() - 34;
        int space = Math.max(96, bottom - y);
        int boneRows = Math.max(4, Math.min(discovery.bones().size(), space / ROW_HEIGHT / 2));
        y = renderTargetList(context, y, new TargetList("Bones", discovery.bones(),
                session.state().selection().boneId(), boneRows, AnimatorUiHitKind.BONE)) + 8;
        int faceRows = Math.max(3, Math.min(discovery.faceElements().size(), (bottom - y) / ROW_HEIGHT));
        renderTargetList(context, y, new TargetList("Face", discovery.faceElements(),
                session.state().selection().faceElementId(), faceRows, AnimatorUiHitKind.FACE));
        graphics.drawString(font, AnimatorUiText.fit(font, session.lastMessage(), context.width()),
                context.x(), gui.bottom() - 22, AnimatorUiTheme.MUTED, false);
    }

    private int renderActionGrid(Context context, int y) {
        int buttonWidth = Math.max(42, (context.width() - 8) / 3);
        int x = context.x();
        smallAction(context, new AnimatorPanelRect(x, y, buttonWidth, 16), "Save", AnimatorUiAction.SAVE);
        smallAction(context, new AnimatorPanelRect(x + buttonWidth + 4, y, buttonWidth, 16), "Auto",
                AnimatorUiAction.AUTOSAVE);
        smallAction(context, new AnimatorPanelRect(x + (buttonWidth + 4) * 2, y, buttonWidth, 16), "Reload",
                AnimatorUiAction.HOT_RELOAD);
        y += 20;
        smallAction(context, new AnimatorPanelRect(x, y, buttonWidth, 16), "Undo", AnimatorUiAction.UNDO);
        smallAction(context, new AnimatorPanelRect(x + buttonWidth + 4, y, buttonWidth, 16), "Redo",
                AnimatorUiAction.REDO);
        smallAction(context, new AnimatorPanelRect(x + (buttonWidth + 4) * 2, y, buttonWidth, 16), "Preview",
                AnimatorUiAction.PREVIEW);
        y += 20;
        smallAction(context, new AnimatorPanelRect(x, y, context.width(), 16), "Clear preview",
                AnimatorUiAction.CLEAR_PREVIEW);
        return y + 16;
    }

    private void smallAction(Context context, AnimatorPanelRect rect, String label, AnimatorUiAction action) {
        buttons.button(new AnimatorToolbarRenderer.DrawContext(context.graphics(), context.font()),
                new AnimatorToolbarRenderer.ButtonSpec(rect, label, false, AnimatorUiHitKind.ACTION, action.name()));
    }

    private int renderSelection(Context context, int y) {
        context.graphics().drawString(context.font(), "Selection", context.x(), y, AnimatorUiTheme.TEXT, false);
        AnimatorForgeSession session = context.session();
        y = infoLine(context, y + 13, "Subject", session.state().selection().subjectId().orElse("-"));
        y = infoLine(context, y, "Clip", session.state().selection().clipId().orElse("-"));
        y = infoLine(context, y, "Mode", session.state().mode().name());
        y = infoLine(context, y, "Tool", session.state().tool().name());
        return infoLine(context, y, "State", session.state().dirty() ? "DIRTY" : "CLEAN");
    }

    private int infoLine(Context context, int y, String label, String value) {
        context.graphics().drawString(context.font(), label + ":", context.x(), y, AnimatorUiTheme.DIM, false);
        context.graphics().drawString(context.font(), AnimatorUiText.fit(context.font(), value, context.width() - 68),
                context.x() + 68, y,
                AnimatorUiTheme.MUTED, false);
        return y + 12;
    }

    private int renderTargetList(Context context, int y, TargetList targetList) {
        context.graphics().drawString(context.font(), targetList.title() + "  " + targetList.values().size(),
                context.x(), y, AnimatorUiTheme.TEXT, false);
        y += 13;
        if (targetList.values().isEmpty()) {
            context.graphics().drawString(context.font(), "No ids discovered", context.x(), y,
                    AnimatorUiTheme.DIM, false);
            return y + ROW_HEIGHT;
        }
        int rows = Math.max(1, Math.min(targetList.values().size(), targetList.maxRows()));
        for (int index = 0; index < rows; index++) {
            y = renderTargetRow(context, y, new TargetRow(targetList.values().get(index),
                    targetList.selected(), targetList.kind()));
        }
        if (targetList.values().size() > rows) {
            context.graphics().drawString(context.font(), "+" + (targetList.values().size() - rows),
                    context.x() + 6, y + 2, AnimatorUiTheme.DIM, false);
            y += ROW_HEIGHT;
        }
        return y;
    }

    private int renderTargetRow(Context context, int y, TargetRow targetRow) {
        boolean active = targetRow.selected().filter(targetRow.value()::equals).isPresent();
        AnimatorPanelRect row = new AnimatorPanelRect(context.x(), y, context.width(), ROW_HEIGHT - 1);
        context.graphics().fill(row.left(), row.top(), row.right(), row.bottom(),
                active ? AnimatorUiTheme.ROW_ACTIVE : AnimatorUiTheme.ROW);
        if (active) {
            context.graphics().fill(row.left(), row.top(), row.left() + 2, row.bottom(), AnimatorUiTheme.GREEN);
        }
        context.graphics().drawString(context.font(), AnimatorUiText.fit(context.font(), targetRow.value(),
                        context.width() - 10),
                row.left() + 6, row.top() + 3, active ? AnimatorUiTheme.TEXT : AnimatorUiTheme.MUTED, false);
        hitboxes.add(row, targetRow.kind(), targetRow.value());
        return y + ROW_HEIGHT;
    }

    private record Context(GuiGraphics graphics, Font font, AnimatorForgeSession session, int x, int width) {
    }

    private record TargetList(String title, List<String> values, Optional<String> selected, int maxRows,
                              AnimatorUiHitKind kind) {
    }

    private record TargetRow(String value, Optional<String> selected, AnimatorUiHitKind kind) {
    }
}
