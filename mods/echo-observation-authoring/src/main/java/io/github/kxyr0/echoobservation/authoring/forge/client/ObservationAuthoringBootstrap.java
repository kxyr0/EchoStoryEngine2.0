package io.github.kxyr0.echoobservation.authoring.forge.client;

import io.github.kxyr0.echoobservation.api.client.ObservationAuthoringReloadEvent;
import io.github.kxyr0.echoobservation.authoring.forge.client.render.ObservationAuthoringHud;
import io.github.kxyr0.echoobservation.authoring.forge.client.render.ObservationAuthoringWorldRenderer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ObservationAuthoringBootstrap {
    private static final ObservationAuthoringRuntime RUNTIME = new ObservationAuthoringRuntime();

    private ObservationAuthoringBootstrap() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ObservationAuthoringKeys::register);
        MinecraftForge.EVENT_BUS.addListener(ObservationAuthoringBootstrap::tick);
        MinecraftForge.EVENT_BUS.addListener(ObservationAuthoringBootstrap::reload);
        MinecraftForge.EVENT_BUS.register(new ObservationAuthoringWorldRenderer(RUNTIME));
        MinecraftForge.EVENT_BUS.register(new ObservationAuthoringHud(RUNTIME));
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) RUNTIME.tick();
    }

    private static void reload(ObservationAuthoringReloadEvent event) {
        RUNTIME.accept(event);
    }
}
