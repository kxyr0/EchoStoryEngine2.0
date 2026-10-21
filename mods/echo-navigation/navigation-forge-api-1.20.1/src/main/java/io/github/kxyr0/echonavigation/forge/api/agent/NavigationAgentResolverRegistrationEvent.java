package io.github.kxyr0.echonavigation.forge.api.agent;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class NavigationAgentResolverRegistrationEvent extends Event {
    private final MinecraftServer server;
    private final NavigationAgentResolverRegistry registry;

    public NavigationAgentResolverRegistrationEvent(MinecraftServer server, NavigationAgentResolverRegistry registry) {
        this.server = Objects.requireNonNull(server, "server");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public MinecraftServer server() {
        return server;
    }

    public NavigationAgentResolverRegistry registry() {
        return registry;
    }
}
