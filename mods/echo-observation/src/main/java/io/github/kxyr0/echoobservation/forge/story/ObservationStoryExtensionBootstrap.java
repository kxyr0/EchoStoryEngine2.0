package io.github.kxyr0.echoobservation.forge.story;

import io.github.kxyr0.echoobservation.api.runtime.ObservationService;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExtensionRegistrationEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ObservationStoryExtensionBootstrap {
    private ObservationStoryExtensionBootstrap() {
    }

    public static void register(IEventBus forgeBus, ObservationService service) {
        forgeBus.addListener((EchoStoryExtensionRegistrationEvent event) ->
                event.registrar().register(new ObservationStoryExtensionFactory(service)));
    }
}
