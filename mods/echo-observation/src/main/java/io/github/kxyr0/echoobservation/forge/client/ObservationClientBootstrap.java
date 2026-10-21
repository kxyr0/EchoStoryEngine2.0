package io.github.kxyr0.echoobservation.forge.client;

import io.github.kxyr0.echoobservation.api.client.ObservationClientLookupEvent;
import io.github.kxyr0.echoobservation.forge.client.projection.ObservationRenderFrame;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ObservationClientBootstrap {
    private ObservationClientBootstrap() {
    }

    public static void register(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.addListener(ObservationClientBootstrap::tick);
        MinecraftForge.EVENT_BUS.addListener(ObservationClientBootstrap::renderLevel);
        MinecraftForge.EVENT_BUS.addListener(ObservationClientBootstrap::render);
        MinecraftForge.EVENT_BUS.addListener(ObservationClientBootstrap::lookup);
        MinecraftForge.EVENT_BUS.addListener(ObservationClientBootstrap::loggedOut);
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) ObservationClientRuntime.instance().tick();
    }

    private static void render(RenderGuiEvent.Post event) {
        ObservationClientRuntime.instance().render(event.getGuiGraphics());
    }

    private static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            ObservationClientRuntime.instance().capture(ObservationRenderFrame.capture(event));
        }
    }

    private static void lookup(ObservationClientLookupEvent event) {
        event.provide(ObservationClientRuntime.instance());
    }

    private static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ObservationClientRuntime.instance().clear();
    }
}
