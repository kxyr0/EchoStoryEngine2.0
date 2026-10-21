package io.github.kxyr0.echoanimationeditor.forge.client;

import io.github.kxyr0.echoanimationeditor.forge.client.command.AnimatorEditorCommands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;

public final class AnimatorEditorClientBootstrap {
    private static final AnimatorEditorClient CLIENT = new AnimatorEditorClient();

    private AnimatorEditorClientBootstrap() {
    }

    public static void bootstrap(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.addListener(AnimatorEditorClientBootstrap::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, AnimatorEditorClientBootstrap::suppressHud);
        MinecraftForge.EVENT_BUS.addListener(AnimatorEditorClientBootstrap::tick);
        MinecraftForge.EVENT_BUS.addListener(AnimatorEditorClientBootstrap::logout);
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        AnimatorEditorCommands.register(event, CLIENT);
    }

    private static void suppressHud(RenderGuiOverlayEvent.Pre event) {
        if (CLIENT.active()) {
            event.setCanceled(true);
        }
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CLIENT.tick();
        }
    }

    private static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        CLIENT.forceClose("client logging out");
    }
}
