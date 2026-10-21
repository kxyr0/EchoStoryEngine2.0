package io.github.kxyr0.echoplayerhud.forge.api;

import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/**
 * Event posted by the HUD after all mod constructors have installed listeners.
 */
public final class HudMetricProviderRegistrationEvent extends Event {
    private final HudMetricProviderRegistrar registrar;

    public HudMetricProviderRegistrationEvent(HudMetricProviderRegistrar registrar) {
        this.registrar = Objects.requireNonNull(registrar, "registrar");
    }

    public HudMetricProviderRegistrar registrar() {
        return registrar;
    }
}
