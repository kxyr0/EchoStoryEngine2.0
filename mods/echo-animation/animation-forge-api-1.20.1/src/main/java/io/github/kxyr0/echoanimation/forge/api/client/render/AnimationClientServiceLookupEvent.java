package io.github.kxyr0.echoanimation.forge.api.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/**
 * Client-only lookup event for the public Echo Animation presentation service.
 */
public final class AnimationClientServiceLookupEvent extends Event {
    private final List<AnimationClientService> providers = new ArrayList<>();

    public void provide(AnimationClientService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<AnimationClientService> providers() {
        return List.copyOf(providers);
    }
}
