package io.github.kxyr0.echoeffects.forge.client;

import io.github.kxyr0.echoeffects.core.contribution.EffectContributionSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public final class ForgeScreenOverlay {
    private final ForgeEffectClientState state;

    public ForgeScreenOverlay(ForgeEffectClientState state) {
        this.state = state;
    }

    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        EffectContributionSnapshot snapshot = state.snapshot();
        fill(graphics, width, height, 0x6A7180, snapshot.screenTintOpacity());
        fill(graphics, width, height, 0x11131A, snapshot.screenVignetteOpacity());
    }

    private void fill(GuiGraphics graphics, int width, int height, int rgb, double opacity) {
        int alpha = Math.max(0, Math.min(160, (int) Math.round(opacity * 255.0D)));
        if (alpha <= 0) {
            return;
        }
        graphics.fill(0, 0, width, height, (alpha << 24) | rgb);
    }
}
