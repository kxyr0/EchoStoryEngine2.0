package io.github.kxyr0.echoplayer.forge.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Synchronous server-scoped lookup event for the public Echo player service.
 */
public final class EchoPlayerServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<EchoPlayerService> providers = new ArrayList<>();

    public EchoPlayerServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(EchoPlayerService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<EchoPlayerService> providers() {
        return List.copyOf(providers);
    }
}
