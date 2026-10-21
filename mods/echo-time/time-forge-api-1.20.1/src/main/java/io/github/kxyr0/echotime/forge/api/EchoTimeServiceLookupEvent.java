package io.github.kxyr0.echotime.forge.api;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class EchoTimeServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private EchoTimeServiceBridge bridge;

    public EchoTimeServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(EchoTimeServiceBridge value) {
        if (bridge != null) throw new IllegalStateException("Multiple Echo Time providers responded.");
        bridge = Objects.requireNonNull(value, "value");
    }

    public Optional<EchoTimeServiceBridge> bridge() {
        return Optional.ofNullable(bridge);
    }
}
