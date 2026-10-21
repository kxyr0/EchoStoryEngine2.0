package io.github.kxyr0.echoeffects.forge.client;

import net.minecraftforge.client.event.ViewportEvent;

public final class ForgeCameraEffects {
    private final ForgeEffectClientState state;

    public ForgeCameraEffects(ForgeEffectClientState state) {
        this.state = state;
    }

    public void onComputeFov(ViewportEvent.ComputeFov event) {
        double offset = state.snapshot().fovOffset();
        if (Math.abs(offset) > 0.0001D) {
            event.setFOV(event.getFOV() + offset);
        }
    }
}
