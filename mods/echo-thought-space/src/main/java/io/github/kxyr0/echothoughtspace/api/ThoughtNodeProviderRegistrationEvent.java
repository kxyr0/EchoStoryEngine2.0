package io.github.kxyr0.echothoughtspace.api;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraftforge.eventbus.api.Event;

public final class ThoughtNodeProviderRegistrationEvent extends Event {
    private final Consumer<ThoughtNodeProvider> registrar;

    public ThoughtNodeProviderRegistrationEvent(Consumer<ThoughtNodeProvider> registrar) {
        this.registrar = Objects.requireNonNull(registrar, "registrar");
    }

    public void register(ThoughtNodeProvider provider) {
        registrar.accept(Objects.requireNonNull(provider, "provider"));
    }
}
