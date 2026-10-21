package io.github.kxyr0.echonpc.forge.api.navigation;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public final class NpcNavigationBodyLookupEvent extends Event {
    private final MinecraftServer server;
    private final ActorId actorId;
    private final List<NpcNavigationBody> bodies = new ArrayList<>();

    public NpcNavigationBodyLookupEvent(MinecraftServer server, ActorId actorId) {
        this.server = Objects.requireNonNull(server, "server");
        this.actorId = Objects.requireNonNull(actorId, "actorId");
    }

    public MinecraftServer server() {
        return server;
    }

    public ActorId actorId() {
        return actorId;
    }

    public void provide(NpcNavigationBody body) {
        bodies.add(Objects.requireNonNull(body, "body"));
    }

    public List<NpcNavigationBody> bodies() {
        return List.copyOf(bodies);
    }
}
