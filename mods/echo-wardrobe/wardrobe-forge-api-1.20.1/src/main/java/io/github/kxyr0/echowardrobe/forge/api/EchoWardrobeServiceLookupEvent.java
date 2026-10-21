package io.github.kxyr0.echowardrobe.forge.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/**
 * Synchronous lookup event for the public Echo Wardrobe service.
 */
public final class EchoWardrobeServiceLookupEvent extends Event {
    private final List<EchoWardrobeService> providers = new ArrayList<>();

    public void provide(EchoWardrobeService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<EchoWardrobeService> providers() {
        return List.copyOf(providers);
    }
}
