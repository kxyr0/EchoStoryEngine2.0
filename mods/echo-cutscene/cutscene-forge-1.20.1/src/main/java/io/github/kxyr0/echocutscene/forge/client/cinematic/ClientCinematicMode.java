package io.github.kxyr0.echocutscene.forge.client.cinematic;

import io.github.kxyr0.echocutscene.forge.client.ClientCameraPlayback;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClientCinematicMode {
    private final ClientCameraPlayback playback;

    public ClientCinematicMode(ClientCameraPlayback playback) {
        this.playback = playback;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (playback.cinematic() && VanillaGuiOverlay.CHAT_PANEL.id().equals(event.getOverlay().id())) {
            event.setCanceled(true);
        }
    }
}
