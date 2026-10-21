package io.github.kxyr0.echovisibility.forge.client;

import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionDescriptor;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderExtensionId;
import io.github.kxyr0.echonpc.forge.api.client.render.NpcClientRenderRegistrationEvent;
import io.github.kxyr0.echovisibility.forge.api.debug.VisibilityClientDebugLookupEvent;
import io.github.kxyr0.echovisibility.forge.client.concealment.VisibilityNpcRenderExtension;
import io.github.kxyr0.echovisibility.forge.client.runtime.VisibilityClientRuntime;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

public final class VisibilityClientBootstrap {
    private VisibilityClientBootstrap() {
    }

    public static void register() {
        NpcClientRenderRegistrationEvent.registerGlobal(
                new NpcClientRenderExtensionDescriptor(
                        new NpcClientRenderExtensionId("echo_visibility:concealment"),
                        "Echo Visibility concealment"),
                VisibilityNpcRenderExtension::new);
        MinecraftForge.EVENT_BUS.addListener(VisibilityClientBootstrap::renderLevel);
        MinecraftForge.EVENT_BUS.addListener(VisibilityClientBootstrap::clientTick);
        MinecraftForge.EVENT_BUS.addListener(VisibilityClientBootstrap::debugLookup);
        MinecraftForge.EVENT_BUS.addListener(VisibilityClientBootstrap::loggedOut);
    }

    private static void renderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == Stage.AFTER_CUTOUT_BLOCKS) {
            VisibilityClientRuntime.instance().render(event.getCamera(), event.getPartialTick());
        }
    }

    private static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            VisibilityClientRuntime.instance().tick();
        }
    }

    private static void debugLookup(VisibilityClientDebugLookupEvent event) {
        event.provide(VisibilityClientRuntime.instance());
    }

    private static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VisibilityClientRuntime.instance().clear();
    }
}
