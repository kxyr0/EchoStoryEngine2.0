package io.github.kxyr0.echobackgroundnpc.forge.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class BackgroundNpcServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<BackgroundNpcService> providers = new ArrayList<>();
    public BackgroundNpcServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }
    public MinecraftServer server() { return server; }
    public void provide(BackgroundNpcService service) { providers.add(Objects.requireNonNull(service, "service")); }
    public List<BackgroundNpcService> providers() { return List.copyOf(providers); }
}
