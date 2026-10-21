package io.github.kxyr0.echothoughtspace.integration.observation;

import io.github.kxyr0.echothoughtspace.api.ThoughtNodeProviderRegistrationEvent;
import net.minecraftforge.common.MinecraftForge;

public final class ObservationThoughtAdapterBootstrap {
    private static final ObservationThoughtNodeProvider PROVIDER = new ObservationThoughtNodeProvider();

    private ObservationThoughtAdapterBootstrap() { }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(ObservationThoughtAdapterBootstrap::provide);
    }

    private static void provide(ThoughtNodeProviderRegistrationEvent event) { event.register(PROVIDER); }
}
