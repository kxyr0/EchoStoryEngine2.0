package io.github.kxyr0.echonavigation.forge.api.backend;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class NavigationBackendRegistrationEvent extends Event {
    private final MinecraftServer server;
    private final NavigationBackendRegistry registry;

    public NavigationBackendRegistrationEvent(MinecraftServer server, NavigationBackendRegistry registry) {
        this.server = Objects.requireNonNull(server, "server");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public MinecraftServer server() {
        return server;
    }

    public NavigationBackendRegistry registry() {
        return registry;
    }
}
