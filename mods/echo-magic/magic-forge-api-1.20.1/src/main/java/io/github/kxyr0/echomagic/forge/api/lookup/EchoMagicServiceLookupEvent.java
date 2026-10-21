package io.github.kxyr0.echomagic.forge.api.lookup;

import io.github.kxyr0.echomagic.forge.api.MagicService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class EchoMagicServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<MagicService> providers = new ArrayList<>();
    public EchoMagicServiceLookupEvent(MinecraftServer server) { this.server = Objects.requireNonNull(server, "server"); }
    public MinecraftServer server() { return server; }
    public void provide(MagicService service) { providers.add(Objects.requireNonNull(service, "service")); }
    public List<MagicService> providers() { return List.copyOf(providers); }
}
