package io.github.kxyr0.echoplayeractor.forge.api.seat;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class PlayerActorSeatProviderRegistrationEvent extends Event {
    private final MinecraftServer server;
    private final PlayerActorSeatProviderRegistry registry;

    public PlayerActorSeatProviderRegistrationEvent(MinecraftServer server, PlayerActorSeatProviderRegistry registry) {
        this.server = Objects.requireNonNull(server, "server");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public MinecraftServer server() {
        return server;
    }

    public PlayerActorSeatProviderRegistry registry() {
        return registry;
    }
}
