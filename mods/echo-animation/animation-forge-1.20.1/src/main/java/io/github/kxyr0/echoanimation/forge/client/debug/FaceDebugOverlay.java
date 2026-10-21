package io.github.kxyr0.echoanimation.forge.client.debug;

import io.github.kxyr0.echoanimation.forge.debug.FaceDebugState;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;

/**
 * HUD hover labels for face debug points.
 */
public final class FaceDebugOverlay {
    private static final AtomicReference<List<FaceDebugPoint>> POINTS = new AtomicReference<>(List.of());
    private static final double HOVER_RADIUS = 14.0D;

    private FaceDebugOverlay() {
    }

    public static void add(List<FaceDebugPoint> points) {
        if (points.isEmpty()) {
            return;
        }
        POINTS.getAndUpdate(existing -> {
            List<FaceDebugPoint> combined = existing.size() > 512 ? new ArrayList<>() : new ArrayList<>(existing);
            combined.addAll(points);
            return List.copyOf(combined);
        });
    }

    public static void renderHud(RenderGuiOverlayEvent.Post event) {
        List<FaceDebugPoint> points = POINTS.getAndSet(List.of());
        if (!FaceDebugState.enabled() || points.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Target target = target(minecraft);
        hover(points, target.x(), target.y()).ifPresent(point ->
                drawLabel(event.getGuiGraphics(), minecraft, point, target));
    }

    private static Optional<FaceDebugPoint> hover(List<FaceDebugPoint> points, int x, int y) {
        double max = HOVER_RADIUS * HOVER_RADIUS;
        return points.stream()
                .filter(point -> point.distanceSq(x, y) <= max)
                .min(java.util.Comparator.comparingDouble(point -> point.distanceSq(x, y)));
    }

    private static Target target(Minecraft minecraft) {
        var window = minecraft.getWindow();
        if (minecraft.screen == null) {
            return new Target(window.getGuiScaledWidth() / 2, window.getGuiScaledHeight() / 2);
        }
        int x = (int) (minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth());
        int y = (int) (minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight());
        return new Target(x, y);
    }

    private static void drawLabel(GuiGraphics graphics, Minecraft minecraft, FaceDebugPoint point, Target target) {
        String text = point.label();
        int x = target.x() + 10;
        int y = target.y() + 10;
        int width = minecraft.font.width(text) + 8;
        graphics.fill(x - 4, y - 4, x + width, y + 12, 0xCC101018);
        graphics.drawString(minecraft.font, text, x, y, point.color() & 0xFFFFFF, false);
    }

    private record Target(int x, int y) {
    }
}
