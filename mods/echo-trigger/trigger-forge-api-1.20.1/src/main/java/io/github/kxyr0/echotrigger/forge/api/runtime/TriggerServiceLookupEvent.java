package io.github.kxyr0.echotrigger.forge.api.runtime;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class TriggerServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private TriggerServiceBridge bridge;

    public TriggerServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(TriggerServiceBridge bridge) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public Optional<TriggerServiceBridge> bridge() {
        return Optional.ofNullable(bridge);
    }
}
