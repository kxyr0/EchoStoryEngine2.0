package io.github.kxyr0.echovisibility.forge.api.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class VisibilityServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<VisibilityServiceBridge> bridges = new ArrayList<>();

    public VisibilityServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(VisibilityServiceBridge bridge) {
        bridges.add(Objects.requireNonNull(bridge, "bridge"));
    }

    public Optional<VisibilityServiceBridge> bridge() {
        return bridges.size() == 1 ? Optional.of(bridges.get(0)) : Optional.empty();
    }

    public int providerCount() {
        return bridges.size();
    }
}
