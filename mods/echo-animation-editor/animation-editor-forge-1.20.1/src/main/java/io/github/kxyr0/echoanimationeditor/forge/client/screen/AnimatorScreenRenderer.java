package io.github.kxyr0.echoanimationeditor.forge.client.screen;

import io.github.kxyr0.echoanimationeditor.api.gizmo.AnimatorTransformDelta;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorEditorMode;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorTool;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorLayout;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorViewport;
import io.github.kxyr0.echoanimationeditor.core.layout.AnimatorGuiMapper;
import io.github.kxyr0.echoanimationeditor.core.timeline.AnimatorTimelineGeometry;
import io.github.kxyr0.echoanimationeditor.forge.client.AnimatorForgeSession;
import io.github.kxyr0.echoanimationeditor.forge.client.screen.ui.AnimatorInspectorRenderer;
import io.github.kxyr0.echoanimationeditor.forge.client.screen.ui.AnimatorToolbarRenderer;
import io.github.kxyr0.echoanimationeditor.forge.client.screen.ui.AnimatorUiAction;
import io.github.kxyr0.echoanimationeditor.forge.client.screen.ui.AnimatorUiHitboxes;
import io.github.kxyr0.echoanimationeditor.forge.client.screen.ui.AnimatorUiTheme;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

final class AnimatorScreenRenderer {
    private final AnimatorGuiMapper mapper = new AnimatorGuiMapper();
    private final AnimatorUiHitboxes hitboxes = new AnimatorUiHitboxes();
    private final AnimatorToolbarRenderer toolbar = new AnimatorToolbarRenderer(hitboxes);
    private final AnimatorInspectorRenderer inspector = new AnimatorInspectorRenderer(toolbar, hitboxes);

    Optional<AnimatorTool> toolAt(double mouseX, double mouseY) { return hitboxes.toolAt(mouseX, mouseY); }

    Optional<AnimatorEditorMode> modeAt(double mouseX, double mouseY) { return hitboxes.modeAt(mouseX, mouseY); }

    Optional<AnimatorUiAction> actionAt(double mouseX, double mouseY) { return hitboxes.actionAt(mouseX, mouseY); }

    Optional<String> boneAt(double mouseX, double mouseY) { return hitboxes.boneAt(mouseX, mouseY); }

    Optional<String> faceElementAt(double mouseX, double mouseY) {
        return hitboxes.faceElementAt(mouseX, mouseY);
    }

    void render(GuiGraphics graphics, Font font, AnimatorForgeSession session, AnimatorViewport viewport,
                AnimatorLayout current) {
        hitboxes.clear();
        fill(graphics, viewport, current.toolbar(), AnimatorUiTheme.PANEL_DARK);
        fill(graphics, viewport, current.timeline(), AnimatorUiTheme.PANEL);
        if (!current.inspectorDrawer()) {
            fill(graphics, viewport, current.inspector(), AnimatorUiTheme.PANEL);
        }
        toolbar.render(graphics, font, session, mapper.toGui(current.toolbar(), viewport));
        renderViewport(graphics, font, session, mapper.toGui(current.viewport(), viewport));
        renderTimeline(graphics, font, session, mapper.toGui(current.timeline(), viewport));
        inspector.render(graphics, font, session, mapper.toGui(current.inspector(), viewport));
    }

    private void renderViewport(GuiGraphics graphics, Font font, AnimatorForgeSession session, AnimatorPanelRect gui) {
        graphics.renderOutline(gui.left(), gui.top(), gui.width(), gui.height(), AnimatorUiTheme.LINE_SOFT);
        AnimatorTransformDelta preview = session.previewDelta();
        int cx = gui.left() + gui.width() / 2 + (int) Math.round(preview.translateX() * 20.0D);
        int cy = gui.top() + gui.height() / 2 - (int) Math.round(preview.translateY() * 20.0D);
        String target = session.state().mode() == AnimatorEditorMode.FACE
                ? session.state().selection().faceElementId().orElse("-")
                : session.state().selection().boneId().orElse("-");
        drawTag(graphics, font, gui.left() + 8, gui.top() + 8, target + "  " + session.activeGizmoHandle());
        renderGizmo(graphics, session.state().tool(), cx, cy);
        int marker = Math.max(3, Math.min(12, (int) Math.round(4.0D * preview.scaleX())));
        graphics.fill(cx - marker, cy - marker, cx + marker + 1, cy + marker + 1, AnimatorUiTheme.BLUE);
    }

    private void renderGizmo(GuiGraphics graphics, AnimatorTool tool, int cx, int cy) {
        if (tool == AnimatorTool.ROTATE) {
            rings(graphics, cx, cy);
            return;
        }
        graphics.hLine(cx, cx + 58, cy, AnimatorUiTheme.GREEN);
        graphics.vLine(cx, cy - 58, cy, AnimatorUiTheme.RED);
        diagonal(graphics, cx, cy);
        if (tool == AnimatorTool.SCALE) {
            graphics.fill(cx + 54, cy - 3, cx + 61, cy + 4, AnimatorUiTheme.GREEN);
            graphics.fill(cx - 3, cy - 61, cx + 4, cy - 54, AnimatorUiTheme.RED);
            graphics.fill(cx - 45, cy + 42, cx - 38, cy + 49, AnimatorUiTheme.BLUE);
            return;
        }
        graphics.renderOutline(cx + 12, cy - 34, 22, 22, AnimatorUiTheme.GREEN);
        graphics.renderOutline(cx + 12, cy + 12, 22, 22, AnimatorUiTheme.BLUE);
        graphics.renderOutline(cx - 34, cy - 34, 22, 22, AnimatorUiTheme.YELLOW);
    }

    private void diagonal(GuiGraphics graphics, int cx, int cy) {
        for (int step = 0; step < 48; step++) {
            graphics.fill(cx - step, cy + step, cx - step + 2, cy + step + 2, AnimatorUiTheme.BLUE);
        }
    }

    private void rings(GuiGraphics graphics, int cx, int cy) {
        graphics.renderOutline(cx - 54, cy - 54, 108, 108, AnimatorUiTheme.YELLOW);
        graphics.renderOutline(cx - 40, cy - 40, 80, 80, AnimatorUiTheme.RED);
        graphics.renderOutline(cx - 26, cy - 26, 52, 52, AnimatorUiTheme.GREEN);
    }

    private void renderTimeline(GuiGraphics graphics, Font font, AnimatorForgeSession session, AnimatorPanelRect gui) {
        int y = gui.top() + gui.height() / 2 + 10;
        int left = gui.left() + 16;
        int right = gui.right() - 16;
        AnimatorTimelineGeometry timeline = new AnimatorTimelineGeometry(120, left, right);
        graphics.drawString(font, "Timeline", left, gui.top() + 10, AnimatorUiTheme.TEXT, false);
        graphics.drawString(font, "Frame " + session.state().currentFrame(), right - 54, gui.top() + 10,
                AnimatorUiTheme.MUTED, false);
        graphics.hLine(left, right, y, AnimatorUiTheme.LINE);
        for (int frame = 0; frame <= 120; frame += 10) {
            renderTimelineTick(graphics, font, timeline, frame, y);
        }
        int playhead = timeline.frameToX(session.state().currentFrame());
        graphics.fill(playhead - 2, y - 10, playhead + 3, y + 11, AnimatorUiTheme.GREEN);
    }

    private void renderTimelineTick(GuiGraphics graphics, Font font, AnimatorTimelineGeometry timeline, int frame,
                                    int y) {
        int tick = timeline.frameToX(frame);
        int height = frame % 30 == 0 ? 7 : 4;
        graphics.vLine(tick, y - height, y + height, AnimatorUiTheme.LINE_SOFT);
        if (frame % 30 == 0) {
            graphics.drawString(font, String.valueOf(frame), tick - 5, y + 12, AnimatorUiTheme.DIM, false);
        }
    }

    private void drawTag(GuiGraphics graphics, Font font, int x, int y, String text) {
        int width = font.width(text) + 10;
        graphics.fill(x, y, x + width, y + 16, 0xAA0A0D10);
        graphics.drawString(font, text, x + 5, y + 4, AnimatorUiTheme.MUTED, false);
    }

    private void fill(GuiGraphics graphics, AnimatorViewport viewport, AnimatorPanelRect rect, int color) {
        AnimatorPanelRect gui = mapper.toGui(rect, viewport);
        graphics.fill(gui.left(), gui.top(), gui.right(), gui.bottom(), color);
    }
}
