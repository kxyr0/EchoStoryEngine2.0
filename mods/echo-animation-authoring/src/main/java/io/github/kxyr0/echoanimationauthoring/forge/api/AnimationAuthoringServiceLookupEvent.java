package io.github.kxyr0.echoanimationauthoring.forge.api;

import io.github.kxyr0.echoanimationauthoring.api.service.AnimationAuthoringService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class AnimationAuthoringServiceLookupEvent extends Event {
    private final List<AnimationAuthoringService> providers = new ArrayList<>();

    public void provide(AnimationAuthoringService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public List<AnimationAuthoringService> providers() {
        return List.copyOf(providers);
    }
}
