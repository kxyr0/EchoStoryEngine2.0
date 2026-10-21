package io.github.kxyr0.echoconcentration.forge.client;

import io.github.kxyr0.echoconcentration.api.lifecycle.ConcentrationServiceLookupEvent;
import io.github.kxyr0.echoconcentration.input.ConcentrationShortcut;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

public final class ConcentrationClientBootstrap {
    private static final ConcentrationClientRuntime RUNTIME = new ConcentrationClientRuntime();

    private ConcentrationClientBootstrap() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ConcentrationKeyMappings::register);
        MinecraftForge.EVENT_BUS.addListener(ConcentrationClientBootstrap::tick);
        MinecraftForge.EVENT_BUS.addListener(ConcentrationClientBootstrap::key);
        MinecraftForge.EVENT_BUS.addListener(ConcentrationClientBootstrap::renderLevel);
        MinecraftForge.EVENT_BUS.addListener(ConcentrationClientBootstrap::lookup);
        MinecraftForge.EVENT_BUS.addListener(ConcentrationClientBootstrap::loggedOut);
        MinecraftForge.EVENT_BUS.register(new ConcentrationInputGuard(RUNTIME));
    }

    private static void key(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && ConcentrationShortcut.accepts(event.getModifiers())) {
            RUNTIME.keyPressed(event.getKey(), event.getScanCode());
        }
    }

    private static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            RUNTIME.capturePlayer(event);
        }
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            RUNTIME.tick();
        }
    }

    private static void lookup(ConcentrationServiceLookupEvent event) {
        event.provide(RUNTIME);
    }

    private static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        RUNTIME.clear();
    }
}
