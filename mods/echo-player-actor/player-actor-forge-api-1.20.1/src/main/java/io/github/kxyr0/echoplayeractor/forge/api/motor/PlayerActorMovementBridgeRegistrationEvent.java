package io.github.kxyr0.echoplayeractor.forge.api.motor;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class PlayerActorMovementBridgeRegistrationEvent extends Event {
    private final MinecraftServer server;
    private final PlayerActorMovementBridgeRegistry registry;

    public PlayerActorMovementBridgeRegistrationEvent(
            MinecraftServer server,
            PlayerActorMovementBridgeRegistry registry
    ) {
        this.server = Objects.requireNonNull(server, "server");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public MinecraftServer server() {
        return server;
    }

    public PlayerActorMovementBridgeRegistry registry() {
        return registry;
    }
}
