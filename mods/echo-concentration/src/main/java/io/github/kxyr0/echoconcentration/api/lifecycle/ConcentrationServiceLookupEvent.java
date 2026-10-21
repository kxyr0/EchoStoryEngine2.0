package io.github.kxyr0.echoconcentration.api.lifecycle;

import io.github.kxyr0.echoconcentration.api.ConcentrationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraftforge.eventbus.api.Event;

public final class ConcentrationServiceLookupEvent extends Event {
    private final List<ConcentrationService> providers = new ArrayList<>();

    public void provide(ConcentrationService service) {
        providers.add(Objects.requireNonNull(service, "service"));
    }

    public Optional<ConcentrationService> service() {
        return providers.size() == 1 ? Optional.of(providers.get(0)) : Optional.empty();
    }
}
