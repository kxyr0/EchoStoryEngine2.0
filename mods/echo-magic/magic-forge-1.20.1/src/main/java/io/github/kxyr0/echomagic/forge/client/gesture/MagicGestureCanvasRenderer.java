package io.github.kxyr0.echomagic.forge.client.gesture;

import net.minecraftforge.client.event.RenderGuiOverlayEvent;

final class MagicGestureCanvasRenderer {
    private final MagicGestureHudRenderer hud = new MagicGestureHudRenderer();

    void renderHud(RenderGuiOverlayEvent.Post event, MagicGestureSession session,
                   MagicGestureTrainingClient training) {
        hud.render(event, session, training);
    }
}
