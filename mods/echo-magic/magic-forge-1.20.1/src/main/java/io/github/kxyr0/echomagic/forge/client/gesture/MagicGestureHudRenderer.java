package io.github.kxyr0.echomagic.forge.client.gesture;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kxyr0.echomagic.core.gesture.MagicGesturePoint;
import io.github.kxyr0.echomagic.forge.config.MagicGestureHudConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;

final class MagicGestureHudRenderer {
    private static final double TRAINING_SCALE_RATIO = 82.0D / 64.0D;

    void render(RenderGuiOverlayEvent.Post event, MagicGestureSession session,
                MagicGestureTrainingClient training) {
        if (session == null && !training.active() && !training.reviewing()) return;
        GuiGraphics graphics = event.getGuiGraphics();
        Minecraft minecraft = Minecraft.getInstance();
        MagicGestureHudConfig.Layout layout = MagicGestureHudConfig.current();
        MagicGestureHudConfig.Position position = layout.position(
                minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
        int x = position.x();
        int y = position.y();
        int width = layout.width();
        int height = layout.height();
        double gestureScale = layout.gestureScale();
        panel(graphics, x, y, width, height);
        List<List<MagicGesturePoint>> learned = training.accepted();
        int[] colors = {0x8864B5F6, 0x88EF70C1, 0x88FFB74D};
        for (int index = 0; index < learned.size(); index++) {
            stroke(graphics, learned.get(index), x + width / 2, y + height / 2,
                    gestureScale * TRAINING_SCALE_RATIO, colors[index % colors.length]);
        }
        if (learned.size() == 3) {
            stroke(graphics, average(learned), x + width / 2, y + height / 2,
                    gestureScale * TRAINING_SCALE_RATIO, 0xE8F4FFF8);
        }
        if (session != null) current(graphics, session, x, y, width, height, gestureScale);
    }

    private static void current(GuiGraphics graphics, MagicGestureSession session, int x, int y,
                                int width, int height, double scale) {
        List<List<MagicGesturePoint>> strokes = session.visibleStrokes();
        for (int index = 0; index < strokes.size(); index++) {
            boolean active = session.drawing() && index == strokes.size() - 1;
            stroke(graphics, strokes.get(index), x + width / 2, y + height / 2, scale,
                    active ? 0xF2F3FFF9 : 0xA06AA99E);
        }
        session.cursor().ifPresent(point -> marker(graphics, point, x + width / 2,
                y + height / 2, scale, 0xA8FFE46B, 4));
        if (strokes.isEmpty() || strokes.get(0).isEmpty()) return;
        marker(graphics, strokes.get(0).get(0), x + width / 2, y + height / 2,
                scale, 0xFFFFFFFF, 3);
    }

    private static void stroke(GuiGraphics graphics, List<MagicGesturePoint> points,
                               int centerX, int centerY, double scale, int color) {
        for (int index = 1; index < points.size(); index++) {
            MagicGesturePoint from = points.get(index - 1);
            MagicGesturePoint to = points.get(index);
            double fromX = centerX + from.x() * scale;
            double fromY = centerY - from.y() * scale;
            double toX = centerX + to.x() * scale;
            double toY = centerY - to.y() * scale;
            int outerAlpha = Math.max(18, (color >>> 24) / 3);
            line(graphics, fromX, fromY, toX, toY, color & 0xFFFFFF | outerAlpha << 24, 4);
            line(graphics, fromX, fromY, toX, toY, color, 2);
        }
    }

    private static void marker(GuiGraphics graphics, MagicGesturePoint point, int centerX,
                               int centerY, double scale, int color, int radius) {
        double x = centerX + point.x() * scale;
        double y = centerY - point.y() * scale;
        double previousX = x + radius;
        double previousY = y;
        for (int index = 1; index <= 12; index++) {
            double angle = Math.PI * 2.0D * index / 12.0D;
            double nextX = x + Math.cos(angle) * radius;
            double nextY = y + Math.sin(angle) * radius;
            line(graphics, previousX, previousY, nextX, nextY, color, 2);
            previousX = nextX;
            previousY = nextY;
        }
    }

    private static void line(GuiGraphics graphics, double fromX, double fromY, double toX,
                             double toY, int color, int thickness) {
        double deltaX = toX - fromX;
        double deltaY = toY - fromY;
        double length = Math.hypot(deltaX, deltaY);
        if (length < 0.01D) return;
        PoseStack poses = graphics.pose();
        poses.pushPose();
        poses.translate((fromX + toX) * 0.5D, (fromY + toY) * 0.5D, 0.0D);
        poses.mulPose(Axis.ZP.rotation((float) Math.atan2(deltaY, deltaX)));
        int halfLength = (int) Math.ceil(length * 0.5D) + 1;
        int halfWidth = Math.max(1, thickness / 2);
        graphics.fill(-halfLength, -halfWidth, halfLength, halfWidth + 1, color);
        poses.popPose();
    }

    private static List<MagicGesturePoint> average(List<List<MagicGesturePoint>> samples) {
        List<MagicGesturePoint> result = new ArrayList<>(samples.get(0).size());
        for (int index = 0; index < samples.get(0).size(); index++) {
            int pointIndex = index;
            double x = samples.stream().mapToDouble(sample -> sample.get(pointIndex).x()).average().orElse(0.0D);
            double y = samples.stream().mapToDouble(sample -> sample.get(pointIndex).y()).average().orElse(0.0D);
            result.add(new MagicGesturePoint(x, y, index));
        }
        return result;
    }

    private static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        roundedFill(graphics, x, y, width, height, 0x0D101318);
        roundedBorder(graphics, x, y, width, height, 0x2876D3C2);
    }

    private static void roundedFill(GuiGraphics graphics, int x, int y,
                                    int width, int height, int color) {
        graphics.fill(x + 6, y, x + width - 6, y + 1, color);
        graphics.fill(x + 3, y + 1, x + width - 3, y + 3, color);
        graphics.fill(x + 1, y + 3, x + width - 1, y + 6, color);
        graphics.fill(x, y + 6, x + width, y + height - 6, color);
        graphics.fill(x + 1, y + height - 6, x + width - 1, y + height - 3, color);
        graphics.fill(x + 3, y + height - 3, x + width - 3, y + height - 1, color);
        graphics.fill(x + 6, y + height - 1, x + width - 6, y + height, color);
    }

    private static void roundedBorder(GuiGraphics graphics, int x, int y,
                                      int width, int height, int color) {
        graphics.fill(x + 6, y, x + width - 6, y + 1, color);
        graphics.fill(x + 3, y + 1, x + 6, y + 2, color);
        graphics.fill(x + width - 6, y + 1, x + width - 3, y + 2, color);
        graphics.fill(x + 1, y + 3, x + 3, y + 6, color);
        graphics.fill(x + width - 3, y + 3, x + width - 1, y + 6, color);
        graphics.fill(x, y + 6, x + 1, y + height - 6, color);
        graphics.fill(x + width - 1, y + 6, x + width, y + height - 6, color);
        graphics.fill(x + 1, y + height - 6, x + 3, y + height - 3, color);
        graphics.fill(x + width - 3, y + height - 6, x + width - 1, y + height - 3, color);
        graphics.fill(x + 3, y + height - 2, x + 6, y + height - 1, color);
        graphics.fill(x + width - 6, y + height - 2, x + width - 3, y + height - 1, color);
        graphics.fill(x + 6, y + height - 1, x + width - 6, y + height, color);
    }
}
