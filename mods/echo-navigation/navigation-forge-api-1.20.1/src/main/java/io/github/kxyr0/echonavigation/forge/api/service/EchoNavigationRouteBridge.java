package io.github.kxyr0.echonavigation.forge.api.service;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

/** Strict event-bus lookup for the optional route-only planning capability. */
public final class EchoNavigationRouteBridge {
    private EchoNavigationRouteBridge() { }

    public static Optional<NavigationSurfaceRouteService> lookup(MinecraftServer server) {
        var event = new EchoNavigationServiceLookupEvent(Objects.requireNonNull(server, "server"));
        MinecraftForge.EVENT_BUS.post(event);
        return event.routeServices().size() == 1 ? Optional.of(event.routeServices().get(0)) : Optional.empty();
    }
}
