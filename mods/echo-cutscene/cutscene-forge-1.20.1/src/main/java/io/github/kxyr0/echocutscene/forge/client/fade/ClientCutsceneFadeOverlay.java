package io.github.kxyr0.echocutscene.forge.client.fade;

import io.github.kxyr0.echocutscene.forge.client.ClientCameraPlayback;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClientCutsceneFadeOverlay {
    private final ClientCameraPlayback playback;

    public ClientCutsceneFadeOverlay(ClientCameraPlayback playback) {
        this.playback = playback;
    }

    @SubscribeEvent
    public void onRender(RenderGuiEvent.Post event) {
        int alpha = Math.round(playback.fadeAlpha() * 255.0F);
        if (alpha <= 0) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(0, 0, event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight(), alpha << 24);
    }
}
