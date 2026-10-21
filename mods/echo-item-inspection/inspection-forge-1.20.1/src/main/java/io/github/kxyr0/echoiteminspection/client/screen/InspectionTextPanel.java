package io.github.kxyr0.echoiteminspection.client.screen;

import io.github.kxyr0.echoiteminspection.client.render.InspectionLayout;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Text owns its scrollbar gestures; mouse wheel remains exclusively model zoom. */
public final class InspectionTextPanel {
    private final Component title;
    private final String description;
    private final InspectionTextScroll scroll = new InspectionTextScroll();
    private List<InspectionTextContent.Line> lines = List.of();
    private InspectionLayout layout;
    private Font font;
    private double guiScale;
    private boolean dragging;
    private int lineHeight;

    public InspectionTextPanel(Component title, String description) {
        this.title = title;
        this.description = description;
    }

    public void resize(Font font, InspectionLayout layout, double guiScale) {
        this.font = font;
        this.layout = layout;
        this.guiScale = guiScale;
        lineHeight = font.lineHeight + 4;
        lines = InspectionTextContent.wrap(font, title, description,
                (int) (layout.textWidth() / layout.textScale()) - 12);
        scroll.resize(lines.size(), (int) (layout.textHeight() / (lineHeight * layout.textScale())));
    }

    public void render(GuiGraphics graphics) {
        graphics.pose().pushPose();
        try {
            float scale = (float) (layout.textScale() / guiScale);
            graphics.pose().scale(scale, scale, 1);
            int x = (int) (layout.textX() / layout.textScale());
            int y = (int) (layout.textY() / layout.textScale());
            for (int index = 0; index < scroll.visibleLines(); index++) {
                int row = scroll.position() + index;
                if (row >= lines.size()) break;
                var line = lines.get(row);
                int lineX = layout.hasDescription() ? x
                        : x + ((int) (layout.textWidth() / layout.textScale()) - font.width(line.text())) / 2;
                font.drawInBatch(line.text(), lineX, y + index * lineHeight,
                        line.title() ? 0xFFF4F0E7 : 0xFFD2D5D8, true, graphics.pose().last().pose(),
                        graphics.bufferSource(), Font.DisplayMode.SEE_THROUGH, 0, 15728880);
            }
        } finally {
            graphics.pose().popPose();
        }
        graphics.flush();
        if (scroll.canScroll()) renderScrollbar(graphics);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int x = (int) ((layout.textX() + layout.textWidth()) / guiScale);
        int top = (int) (layout.textY() / guiScale);
        int height = (int) (layout.textHeight() / guiScale);
        int width = Math.max(1, (int) (layout.textScale() * 2 / guiScale));
        int thumb = Math.max(6, (int) (height * scroll.visibleFraction()));
        int y = top + (int) ((height - thumb) * scroll.fraction());
        graphics.fill(x, top, x + width, top + height, 0x40FFFFFF);
        graphics.fill(x, y, x + width, y + thumb, 0xC8FFFFFF);
    }

    public boolean mouseClicked(double x, double y, int button) {
        double physicalX = x * guiScale;
        double physicalY = y * guiScale;
        boolean inside = physicalX >= layout.textX() && physicalX <= layout.textX() + layout.textWidth() + 12
                && physicalY >= layout.textY() && physicalY <= layout.textY() + layout.textHeight();
        dragging = button == 0 && inside && scroll.canScroll()
                && physicalX >= layout.textX() + layout.textWidth() - 10;
        if (dragging) seek(physicalY);
        return inside;
    }

    public boolean mouseDragged(double y, int button) {
        if (!dragging || button != 0) return false;
        seek(y * guiScale);
        return true;
    }

    private void seek(double y) {
        scroll.seek((y - layout.textY()) / layout.textHeight());
    }

    public void release() { dragging = false; }

    public boolean keyPressed(int key) {
        if (!scroll.canScroll()) return false;
        switch (key) {
            case GLFW.GLFW_KEY_UP -> scroll.move(-1);
            case GLFW.GLFW_KEY_DOWN -> scroll.move(1);
            case GLFW.GLFW_KEY_PAGE_UP -> scroll.page(-1);
            case GLFW.GLFW_KEY_PAGE_DOWN -> scroll.page(1);
            case GLFW.GLFW_KEY_HOME -> scroll.seek(0);
            case GLFW.GLFW_KEY_END -> scroll.seek(1);
            default -> { return false; }
        }
        return true;
    }

    public String title() { return title.getString(); }
    public String description() { return description; }
    public int lineCount() { return lines.size(); }
    public InspectionTextScroll scroll() { return scroll; }
    public InspectionLayout layout() { return layout; }
}
