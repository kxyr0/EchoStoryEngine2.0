package io.github.kxyr0.echoplayervitals.forge.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Synchronous server-scoped lookup event for the public Vitals service.
 */
public final class EchoPlayerVitalsServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<EchoPlayerVitalsService> providers = new ArrayList<>();

    public EchoPlayerVitalsServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(EchoPlayerVitalsService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<EchoPlayerVitalsService> providers() {
        return List.copyOf(providers);
    }
}
