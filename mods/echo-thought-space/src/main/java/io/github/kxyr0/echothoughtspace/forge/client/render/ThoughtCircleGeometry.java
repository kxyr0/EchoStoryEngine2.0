package io.github.kxyr0.echothoughtspace.forge.client.render;

import io.github.kxyr0.echothoughtspace.core.canvas.CanvasPoint;
import net.minecraft.client.gui.GuiGraphics;

public final class ThoughtCircleGeometry {
    private ThoughtCircleGeometry() { }

    public static void drawFilled(GuiGraphics graphics, CanvasPoint center, double radius, int color) {
        int top = (int) Math.floor(center.y() - radius);
        int bottom = (int) Math.ceil(center.y() + radius);
        for (int y = top; y < bottom; y++) {
            double dy = y + 0.5D - center.y();
            if (Math.abs(dy) > radius) continue;
            double halfWidth = Math.sqrt(radius * radius - dy * dy);
            int left = (int) Math.ceil(center.x() - halfWidth - 0.5D);
            int right = (int) Math.floor(center.x() + halfWidth - 0.5D) + 1;
            if (right > left) graphics.fill(left, y, right, y + 1, color);
        }
    }
}
