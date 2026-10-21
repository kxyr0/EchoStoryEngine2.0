package io.github.kxyr0.echoplayersleep.forge.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class PlayerSleepServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private PlayerSleepServiceBridge bridge;

    public PlayerSleepServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(PlayerSleepServiceBridge bridge) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public Optional<PlayerSleepServiceBridge> bridge() {
        return Optional.ofNullable(bridge);
    }
}
