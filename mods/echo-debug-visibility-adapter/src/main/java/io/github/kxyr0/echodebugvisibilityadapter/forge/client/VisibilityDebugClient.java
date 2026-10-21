package io.github.kxyr0.echodebugvisibilityadapter.forge.client;

import io.github.kxyr0.echodebugvisibilityadapter.forge.client.command.VisibilityDebugCommands;
import io.github.kxyr0.echodebugvisibilityadapter.forge.client.hud.VisibilityHudRenderer;
import io.github.kxyr0.echodebugvisibilityadapter.forge.client.render.VisibilityWorldRenderer;
import net.minecraftforge.common.MinecraftForge;

public final class VisibilityDebugClient {
    private VisibilityDebugClient() {
    }

    public static void register() {
        VisibilityDebugSettings settings = new VisibilityDebugSettings();
        VisibilityDebugAccess access = new VisibilityDebugAccess();
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.RegisterClientCommandsEvent event) ->
                VisibilityDebugCommands.register(event, settings, access));
        MinecraftForge.EVENT_BUS.register(new VisibilityWorldRenderer(settings, access));
        MinecraftForge.EVENT_BUS.register(new VisibilityHudRenderer(settings, access));
    }
}
