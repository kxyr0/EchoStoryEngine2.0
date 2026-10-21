package io.github.kxyr0.echoseating.forge.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class SeatingServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private SeatingServiceBridge bridge;

    public SeatingServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(SeatingServiceBridge bridge) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public Optional<SeatingServiceBridge> bridge() {
        return Optional.ofNullable(bridge);
    }
}
