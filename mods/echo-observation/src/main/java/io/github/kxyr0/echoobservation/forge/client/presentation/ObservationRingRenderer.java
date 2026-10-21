package io.github.kxyr0.echoobservation.forge.client.presentation;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class ObservationRingRenderer {
    private static final int SEGMENTS = 64;
    private static final double[] COS = new double[SEGMENTS];
    private static final double[] SIN = new double[SEGMENTS];

    static {
        for (int index = 0; index < SEGMENTS; index++) {
            double angle = Math.PI * 2.0D * index / SEGMENTS - Math.PI / 2.0D;
            COS[index] = Math.cos(angle);
            SIN[index] = Math.sin(angle);
        }
    }

    public void render(GuiGraphics graphics, List<ProjectedObservation> observations,
            ProjectedObservation targeted, double progress) {
        Minecraft minecraft = Minecraft.getInstance();
        double scaleX = minecraft.getWindow().getGuiScaledWidth()
                / (double) minecraft.getWindow().getWidth();
        double scaleY = minecraft.getWindow().getGuiScaledHeight()
                / (double) minecraft.getWindow().getHeight();
        graphics.pose().pushPose();
        graphics.pose().scale((float) scaleX, (float) scaleY, 1.0F);
        observations.forEach(value -> ring(graphics, value, value == targeted,
                value == targeted ? progress : 0.0D));
        graphics.pose().popPose();
    }

    private void ring(GuiGraphics graphics, ProjectedObservation value, boolean targeted, double progress) {
        double centerX = value.point().framebufferX();
        double centerY = value.point().framebufferY();
        double radius = value.physicalRadius();
        double resolved = Math.max(0.0D, Math.min(1.0D, progress));
        if (targeted && resolved > 0.0D) {
            fillProgress(graphics, centerX, centerY, radius, resolved, value.opacity());
        }
        int baseAlpha = targeted ? 255 : 184;
        int color = color((int) Math.round(baseAlpha * value.opacity()));
        for (int index = 0; index < SEGMENTS; index++) {
            int x = (int) Math.round(centerX + COS[index] * radius);
            int y = (int) Math.round(centerY + SIN[index] * radius);
            graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
        }
    }

    private void fillProgress(GuiGraphics graphics, double centerX, double centerY,
            double radius, double progress, double opacity) {
        int centerPixelX = (int) Math.round(centerX);
        int centerPixelY = (int) Math.round(centerY);
        int pixelRadius = ObservationCenterProgress.radius(radius, progress);
        if (pixelRadius <= 0) return;
        int fillColor = color((int) Math.round(210.0D * opacity));
        for (int y = -pixelRadius; y <= pixelRadius; y++) {
            int extent = (int) Math.floor(Math.sqrt(pixelRadius * pixelRadius - y * y));
            graphics.fill(centerPixelX - extent, centerPixelY + y,
                    centerPixelX + extent + 1, centerPixelY + y + 1, fillColor);
        }
    }

    private int color(int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | 0x00ffffff;
    }
}
