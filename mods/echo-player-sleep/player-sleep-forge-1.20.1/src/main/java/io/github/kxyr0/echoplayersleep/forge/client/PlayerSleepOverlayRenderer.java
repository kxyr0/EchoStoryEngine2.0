package io.github.kxyr0.echoplayersleep.forge.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public final class PlayerSleepOverlayRenderer {
    private static final int BLACK = 0xFF000000;
    private final ClientSleepTransitionMirror mirror;

    public PlayerSleepOverlayRenderer(ClientSleepTransitionMirror mirror) {
        this.mirror = mirror;
    }

    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        render(graphics, width, height);
    }

    public void render(GuiGraphics graphics, int width, int height) {
        double coverage = mirror.coverage();
        if (coverage <= 0.0D) {
            return;
        }
        int halfHeight = (int) Math.ceil(height * Math.min(1.0D, coverage) / 2.0D);
        graphics.fill(0, 0, width, halfHeight, BLACK);
        graphics.fill(0, height - halfHeight, width, height, BLACK);
        if (coverage >= 1.0D) {
            graphics.fill(0, 0, width, height, BLACK);
        }
    }
}
