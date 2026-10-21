package io.github.kxyr0.echoanimation.forge.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Synchronous server-scoped lookup event for the public Echo Animation service.
 */
public final class AnimationServiceLookupEvent extends Event {
    private final MinecraftServer server;
    private final List<AnimationService> providers = new ArrayList<>();

    public AnimationServiceLookupEvent(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public MinecraftServer server() {
        return server;
    }

    public void provide(AnimationService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<AnimationService> providers() {
        return List.copyOf(providers);
    }
}

