package io.github.kxyr0.echonavigation.forge.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class EchoNavigationServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<NavigationService> services = new ArrayList<>();
    private final List<NavigationQueryService> queryServices = new ArrayList<>();
    private final List<NavigationSurfaceRouteService> routeServices = new ArrayList<>();

    public EchoNavigationServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(NavigationService service, NavigationQueryService queryService) {
        services.add(Objects.requireNonNull(service, "service"));
        queryServices.add(Objects.requireNonNull(queryService, "queryService"));
    }

    public void provide(NavigationService service, NavigationQueryService queryService,
            NavigationSurfaceRouteService routeService) {
        provide(service, queryService);
        routeServices.add(Objects.requireNonNull(routeService, "routeService"));
    }

    public List<NavigationService> services() {
        return List.copyOf(services);
    }

    public List<NavigationQueryService> queryServices() {
        return List.copyOf(queryServices);
    }

    public List<NavigationSurfaceRouteService> routeServices() { return List.copyOf(routeServices); }
}
