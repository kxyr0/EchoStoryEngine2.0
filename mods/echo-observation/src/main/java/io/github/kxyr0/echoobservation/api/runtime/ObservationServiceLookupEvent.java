package io.github.kxyr0.echoobservation.api.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class ObservationServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<ObservationService> providers = new ArrayList<>();

    public ObservationServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() { return server; }
    public void provide(ObservationService service) { providers.add(Objects.requireNonNull(service, "service")); }
    public Optional<ObservationService> service() {
        return providers.size() == 1 ? Optional.of(providers.get(0)) : Optional.empty();
    }
}
