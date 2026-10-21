package io.github.kxyr0.echoplayeractor.forge.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class PlayerActorServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<PlayerActorService> services = new ArrayList<>();
    private final List<PlayerActorQueryService> queryServices = new ArrayList<>();
    private final List<PlayerControlLeaseService> leaseServices = new ArrayList<>();

    public PlayerActorServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(
            PlayerActorService service,
            PlayerActorQueryService queryService,
            PlayerControlLeaseService leaseService
    ) {
        services.add(Objects.requireNonNull(service, "service"));
        queryServices.add(Objects.requireNonNull(queryService, "queryService"));
        leaseServices.add(Objects.requireNonNull(leaseService, "leaseService"));
    }

    public List<PlayerActorService> services() {
        return List.copyOf(services);
    }

    public List<PlayerActorQueryService> queryServices() {
        return List.copyOf(queryServices);
    }

    public List<PlayerControlLeaseService> leaseServices() {
        return List.copyOf(leaseServices);
    }
}
