package io.github.kxyr0.echonpcbackpack.forge.api;

import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class NpcBackpackServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<NpcBackpackService> services = new ArrayList<>();

    public NpcBackpackServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(NpcBackpackService service) {
        services.add(Objects.requireNonNull(service, "service"));
    }

    public List<NpcBackpackService> services() {
        return List.copyOf(services);
    }
}
