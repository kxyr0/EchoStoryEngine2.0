package io.github.kxyr0.echonpc.forge.api;

import io.github.kxyr0.echonpc.api.service.NpcService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Synchronous server-scoped lookup event for the public Echo NPC service.
 */
public final class EchoNpcServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<NpcService> providers = new ArrayList<>();

    public EchoNpcServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(NpcService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<NpcService> providers() {
        return List.copyOf(providers);
    }
}
