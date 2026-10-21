package io.github.kxyr0.echonpcinteraction.forge.api.event;

import io.github.kxyr0.echonpcinteraction.forge.api.provider.NpcInteractionProviderRegistry;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

public final class NpcInteractionProviderRegistrationEvent extends Event {
    private final NpcInteractionProviderRegistry registry;

    public NpcInteractionProviderRegistrationEvent(NpcInteractionProviderRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public NpcInteractionProviderRegistry registry() {
        return registry;
    }
}
