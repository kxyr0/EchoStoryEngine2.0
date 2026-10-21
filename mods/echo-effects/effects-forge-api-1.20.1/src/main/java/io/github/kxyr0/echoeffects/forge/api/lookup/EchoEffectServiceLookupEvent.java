package io.github.kxyr0.echoeffects.forge.api.lookup;

import io.github.kxyr0.echoeffects.forge.api.EffectService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Synchronous server-scoped lookup event for the public Echo Effects service.
 */
public final class EchoEffectServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<EffectService> providers = new ArrayList<>();

    public EchoEffectServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(EffectService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<EffectService> providers() {
        return List.copyOf(providers);
    }
}
