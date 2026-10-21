package io.github.kxyr0.echothoughtspace.forge.client;

import io.github.kxyr0.echothoughtspace.forge.client.input.ThoughtSpaceInputGuard;
import io.github.kxyr0.echothoughtspace.forge.command.ThoughtSpaceClientCommands;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

public final class ThoughtSpaceClientBootstrap {
    private static final ThoughtSpaceRuntime RUNTIME = new ThoughtSpaceRuntime();

    private ThoughtSpaceClientBootstrap() { }

    public static void register(IEventBus modBus) {
        modBus.addListener(ThoughtSpaceKeys::register);
        MinecraftForge.EVENT_BUS.addListener(ThoughtSpaceClientBootstrap::key);
        MinecraftForge.EVENT_BUS.addListener(ThoughtSpaceClientBootstrap::tick);
        MinecraftForge.EVENT_BUS.addListener(ThoughtSpaceClientBootstrap::logout);
        MinecraftForge.EVENT_BUS.addListener(ThoughtSpaceClientBootstrap::commands);
        MinecraftForge.EVENT_BUS.register(new ThoughtSpaceInputGuard(RUNTIME));
    }

    private static void key(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS
                && ThoughtSpaceKeys.TOGGLE.matches(event.getKey(), event.getScanCode())) {
            RUNTIME.toggleEdge();
        }
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) RUNTIME.tick();
    }

    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) { RUNTIME.logout(); }
    private static void commands(RegisterClientCommandsEvent event) {
        ThoughtSpaceClientCommands.register(event, RUNTIME);
    }
}
