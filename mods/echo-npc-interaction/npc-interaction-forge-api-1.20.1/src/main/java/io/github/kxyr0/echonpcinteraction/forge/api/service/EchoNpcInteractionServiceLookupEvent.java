package io.github.kxyr0.echonpcinteraction.forge.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class EchoNpcInteractionServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<NpcInteractionService> services = new ArrayList<>();
    private final List<NpcInteractionQueryService> queryServices = new ArrayList<>();

    public EchoNpcInteractionServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(NpcInteractionService service, NpcInteractionQueryService queryService) {
        services.add(Objects.requireNonNull(service, "service"));
        queryServices.add(Objects.requireNonNull(queryService, "queryService"));
    }

    public List<NpcInteractionService> services() {
        return List.copyOf(services);
    }

    public List<NpcInteractionQueryService> queryServices() {
        return List.copyOf(queryServices);
    }
}
