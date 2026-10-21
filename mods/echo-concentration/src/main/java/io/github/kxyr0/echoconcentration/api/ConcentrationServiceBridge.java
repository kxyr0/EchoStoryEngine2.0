package io.github.kxyr0.echoconcentration.api;

import io.github.kxyr0.echoconcentration.api.lifecycle.ConcentrationServiceLookupEvent;
import java.util.Optional;
import net.minecraftforge.common.MinecraftForge;

public final class ConcentrationServiceBridge {
    private ConcentrationServiceBridge() {
    }

    public static Optional<ConcentrationService> lookup() {
        ConcentrationServiceLookupEvent event = new ConcentrationServiceLookupEvent();
        MinecraftForge.EVENT_BUS.post(event);
        return event.service();
    }
}
