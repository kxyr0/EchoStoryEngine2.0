package io.github.kxyr0.echocutscene.forge.api.event;

import io.github.kxyr0.echocutscene.forge.api.service.CutsceneAuthoringService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class CutsceneAuthoringServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<CutsceneAuthoringService> providers = new ArrayList<>();

    public CutsceneAuthoringServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(CutsceneAuthoringService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<CutsceneAuthoringService> providers() {
        return List.copyOf(providers);
    }
}
