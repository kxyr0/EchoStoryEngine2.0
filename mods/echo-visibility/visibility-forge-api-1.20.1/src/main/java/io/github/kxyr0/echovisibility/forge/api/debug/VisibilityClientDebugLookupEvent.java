package io.github.kxyr0.echovisibility.forge.api.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraftforge.eventbus.api.Event;

public final class VisibilityClientDebugLookupEvent extends Event {
    private final List<VisibilityClientDebugView> providers = new ArrayList<>();

    public void provide(VisibilityClientDebugView provider) {
        providers.add(Objects.requireNonNull(provider, "provider"));
    }

    public Optional<VisibilityClientDebugView> view() {
        return providers.size() == 1 ? Optional.of(providers.get(0)) : Optional.empty();
    }

    public int providerCount() {
        return providers.size();
    }
}
