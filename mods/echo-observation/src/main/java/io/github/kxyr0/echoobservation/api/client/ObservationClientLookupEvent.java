package io.github.kxyr0.echoobservation.api.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraftforge.eventbus.api.Event;

public final class ObservationClientLookupEvent extends Event {
    private final List<ObservationClientView> providers = new ArrayList<>();

    public void provide(ObservationClientView view) { providers.add(Objects.requireNonNull(view, "view")); }
    public Optional<ObservationClientView> view() {
        return providers.size() == 1 ? Optional.of(providers.get(0)) : Optional.empty();
    }
}
