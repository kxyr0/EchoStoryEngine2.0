package io.github.kxyr0.echotrigger.forge.api.authoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class TriggerAuthoringServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<TriggerAuthoringService> providers = new ArrayList<>();

    public TriggerAuthoringServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(TriggerAuthoringService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<TriggerAuthoringService> providers() {
        return List.copyOf(providers);
    }
}
