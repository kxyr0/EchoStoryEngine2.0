package io.github.kxyr0.echoanimationauthoring.client;

import io.github.kxyr0.echoanimationauthoring.editor.AuthoringLabels;
import io.github.kxyr0.echoanimationauthoring.editor.EditorSession;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Minimal in-game authoring surface used by the development-only editor mod.
 */
public final class AuthoringScreen extends Screen {
    private static final int PANEL_WIDTH = 184;
    private static final int GAP = 10;
    private static final int TEXT = 0xFFECE7DC;
    private static final int MUTED = 0xFFAAA69A;
    private static final int PANEL = 0xE51D2026;
    private static final int PREVIEW_BG = 0xFF242832;
    private static final int ACCENT = 0xFF65C18C;
    private static final int WARNING = 0xFFE9B44C;
    private EditorSession session;

    public AuthoringScreen(EditorSession session) {
        super(Component.literal(AuthoringLabels.OPEN_EDITOR));
        this.session = session;
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = GAP;
        int y = 40;
        addToolButton(x, y, AuthoringLabels.TRANSLATE);
        addToolButton(x, y + 24, AuthoringLabels.PIVOT);
        addToolButton(x, y + 48, AuthoringLabels.RE_PARENT);
        addSmallButton(x, y + 84, AuthoringLabels.COMPARE_DEFAULT, button -> toggleCompare());
        addSmallButton(x, height - 56, "<", button -> scrubTimeline(-1));
        addSmallButton(x + 42, height - 56, ">", button -> scrubTimeline(1));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderPanels(graphics);
        renderHeader(graphics);
        renderEditorText(graphics);
        renderPreview(graphics, mouseX, mouseY);
        renderTimeline(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void addToolButton(int x, int y, String label) {
        addRenderableWidget(Button.builder(Component.literal(label), button -> selectTool(label))
                .bounds(x, y, PANEL_WIDTH - GAP * 2, 20)
                .build());
    }

    private void addSmallButton(int x, int y, String label, Button.OnPress action) {
        addRenderableWidget(Button.builder(Component.literal(label), action).bounds(x, y, 38, 20).build());
    }

    private void selectTool(String tool) {
        session = session.selectTool(tool);
    }

    private void toggleCompare() {
        session = session.toggleCompareDefault();
    }

    private void scrubTimeline(int frameDelta) {
        session = session.scrubTimeline(frameDelta);
    }

    private void renderPanels(GuiGraphics graphics) {
        graphics.fill(0, 0, width, height, 0xF0101115);
        graphics.fill(0, 0, PANEL_WIDTH, height, PANEL);
        graphics.fill(PANEL_WIDTH + GAP, GAP, width - GAP, height - 62, PREVIEW_BG);
        graphics.renderOutline(PANEL_WIDTH + GAP, GAP, width - GAP * 2 - PANEL_WIDTH, height - 72, 0xFF575F6D);
        graphics.fill(PANEL_WIDTH + GAP, height - 52, width - GAP, height - GAP, PANEL);
    }

    private void renderHeader(GuiGraphics graphics) {
        graphics.drawString(font, AuthoringLabels.OPEN_EDITOR, GAP, 14, TEXT, false);
        int statusColor = session.writeEnabled() ? ACCENT : WARNING;
        graphics.drawString(font, session.writeStateLabel(), GAP, 26, statusColor, false);
    }

    private void renderEditorText(GuiGraphics graphics) {
        int y = 134;
        graphics.drawString(font, AuthoringLabels.HIERARCHY, GAP, y, TEXT, false);
        graphics.drawString(font, "root/head", GAP + 8, y + 14, MUTED, false);
        graphics.drawString(font, "head/upper_eyelids", GAP + 8, y + 26, MUTED, false);
        graphics.drawString(font, "head/lower_eyelids", GAP + 8, y + 38, MUTED, false);
        graphics.drawString(font, AuthoringLabels.LAYERS, GAP, y + 62, TEXT, false);
        graphics.drawString(font, "upper_eyelids.png", GAP + 8, y + 76, MUTED, false);
        graphics.drawString(font, "lower_eyelids.png", GAP + 8, y + 88, MUTED, false);
        if (session.hasGlasses()) {
            graphics.drawString(font, "glasses.png", GAP + 8, y + 100, MUTED, false);
        }
        graphics.drawString(font, session.workspaceLabel(), GAP, height - 30, MUTED, false);
    }

    private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = PANEL_WIDTH + GAP * 3;
        int top = GAP * 4;
        int size = Math.min(width - left - GAP * 3, height - 140);
        int centerX = left + size / 2;
        int centerY = top + size / 2;
        renderPreviewGrid(graphics, left, top, size);
        renderHead(graphics, centerX, centerY, size);
        renderBoneOverlay(graphics, centerX, centerY, size);
        renderPreviewLabels(graphics, left, top, mouseX, mouseY);
    }

    private void renderPreviewGrid(GuiGraphics graphics, int left, int top, int size) {
        for (int offset = 0; offset <= size; offset += 24) {
            graphics.vLine(left + offset, top, top + size, 0x223B4250);
            graphics.hLine(left, left + size, top + offset, 0x223B4250);
        }
    }

    private void renderHead(GuiGraphics graphics, int centerX, int centerY, int size) {
        int head = Math.max(92, size / 3);
        graphics.fill(centerX - head / 2, centerY - head / 2, centerX + head / 2, centerY + head / 2, 0xFFD8B48A);
        graphics.fill(centerX - head / 4, centerY - 14, centerX - 12, centerY - 7, 0xFF2D2A28);
        graphics.fill(centerX + 12, centerY - 14, centerX + head / 4, centerY - 7, 0xFF2D2A28);
        graphics.fill(centerX - head / 4, centerY + 22, centerX + head / 4, centerY + 26, 0xFF7D3B3B);
        graphics.renderOutline(centerX - head / 2, centerY - head / 2, head, head, 0xFF3B2E24);
        if (session.hasGlasses() && !session.compareDefault()) {
            graphics.renderOutline(centerX - head / 3, centerY - 20, head / 3, 22, 0xFFB4D7F2);
            graphics.renderOutline(centerX, centerY - 20, head / 3, 22, 0xFFB4D7F2);
        }
    }

    private void renderBoneOverlay(GuiGraphics graphics, int centerX, int centerY, int size) {
        int spineTop = centerY - size / 5;
        int spineBottom = centerY + size / 5;
        graphics.vLine(centerX, spineTop, spineBottom, ACCENT);
        graphics.hLine(centerX - 58, centerX + 58, centerY - 18, ACCENT);
        graphics.fill(centerX - 3, centerY - 3, centerX + 4, centerY + 4, 0xFFFFFFFF);
        int pivotColor = AuthoringLabels.PIVOT.equals(session.selectedTool()) ? WARNING : 0xFF8DD6A5;
        graphics.fill(centerX - 62, centerY - 22, centerX - 54, centerY - 14, pivotColor);
        graphics.fill(centerX + 54, centerY - 22, centerX + 62, centerY - 14, pivotColor);
    }

    private void renderPreviewLabels(GuiGraphics graphics, int left, int top, int mouseX, int mouseY) {
        graphics.drawString(font, AuthoringLabels.PREVIEW, left, top - 18, TEXT, false);
        if (session.hasGlasses()) {
            graphics.drawString(font, AuthoringLabels.HIDE_GLASSES + ": " + session.compareDefault(), left + 120,
                    top - 18, MUTED, false);
        }
        graphics.drawString(font, mouseX + ", " + mouseY, left, top + 6, 0x779A9A9A, false);
    }

    private void renderTimeline(GuiGraphics graphics) {
        int left = PANEL_WIDTH + GAP * 3;
        int right = width - GAP * 3;
        int y = height - 32;
        int frameX = left + (right - left) * session.timelineFrame() / EditorSession.MAX_TIMELINE_FRAME;
        graphics.drawString(font, AuthoringLabels.TIMELINE, left, y - 16, TEXT, false);
        graphics.hLine(left, right, y, 0xFF6C7480);
        graphics.fill(frameX - 2, y - 8, frameX + 3, y + 9, ACCENT);
        graphics.drawString(font, AuthoringLabels.FRAME + ": " + session.timelineFrame(), right - 70, y - 16, TEXT,
                false);
    }
}
