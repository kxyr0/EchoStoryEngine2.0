package io.github.kxyr0.echoplayerhud.forge.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/**
 * Synchronous lookup event for the public HUD service.
 */
public final class PlayerHudServiceLookupEvent extends Event {
    private final List<EchoPlayerHudService> providers = new ArrayList<>();

    public void provide(EchoPlayerHudService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<EchoPlayerHudService> providers() {
        return List.copyOf(providers);
    }
}
