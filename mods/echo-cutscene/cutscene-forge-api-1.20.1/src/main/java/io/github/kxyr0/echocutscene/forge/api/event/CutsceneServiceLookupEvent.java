package io.github.kxyr0.echocutscene.forge.api.event;

import io.github.kxyr0.echocutscene.forge.api.service.CutsceneService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class CutsceneServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<CutsceneService> providers = new ArrayList<>();

    public CutsceneServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(CutsceneService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<CutsceneService> providers() {
        return List.copyOf(providers);
    }
}
