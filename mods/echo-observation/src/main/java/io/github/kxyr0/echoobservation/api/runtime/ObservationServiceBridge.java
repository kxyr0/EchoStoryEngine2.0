package io.github.kxyr0.echoobservation.api.runtime;

import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

public final class ObservationServiceBridge {
    private ObservationServiceBridge() {
    }

    public static Optional<ObservationService> lookup(MinecraftServer server) {
        ObservationServiceLookupEvent event = new ObservationServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(event);
        return event.service();
    }
}
