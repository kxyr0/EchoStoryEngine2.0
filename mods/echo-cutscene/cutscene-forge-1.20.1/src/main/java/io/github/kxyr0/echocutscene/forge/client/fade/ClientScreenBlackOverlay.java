package io.github.kxyr0.echocutscene.forge.client.fade;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClientScreenBlackOverlay {
    private float startAlpha;
    private float targetAlpha;
    private long startedAtMillis;
    private long durationMillis;

    public void set(float canvasGroup, long timeMillis) {
        long now = Util.getMillis();
        startAlpha = alphaAt(now);
        targetAlpha = clamp(canvasGroup);
        startedAtMillis = now;
        durationMillis = Math.max(0L, timeMillis);
    }

    public boolean active() {
        return alphaAt(Util.getMillis()) > 0.0F;
    }

    public void clear() {
        startAlpha = 0.0F;
        targetAlpha = 0.0F;
        startedAtMillis = 0L;
        durationMillis = 0L;
    }

    @SubscribeEvent
    public void onRender(RenderGuiEvent.Post event) {
        float alpha = alphaAt(Util.getMillis());
        if (alpha <= 0.0F) {
            return;
        }
        int alphaByte = Math.min(255, Math.max(0, Math.round(alpha * 255.0F)));
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(0, 0, event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(), alphaByte << 24);
    }

    private float alphaAt(long nowMillis) {
        if (durationMillis <= 0L) {
            return targetAlpha;
        }
        float progress = Math.min(1.0F, Math.max(0.0F,
                (float) (nowMillis - startedAtMillis) / (float) durationMillis));
        float smoothed = progress * progress * (3.0F - 2.0F * progress);
        return startAlpha + (targetAlpha - startAlpha) * smoothed;
    }

    private static float clamp(float value) {
        return Math.min(1.0F, Math.max(0.0F, value));
    }
}
