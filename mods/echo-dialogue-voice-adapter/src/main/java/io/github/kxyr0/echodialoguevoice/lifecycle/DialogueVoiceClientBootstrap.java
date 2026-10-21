package io.github.kxyr0.echodialoguevoice.lifecycle;

import io.github.kxyr0.echodialoguevoice.command.DialogueVoiceClientCommands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;

/** Installs client-only lifecycle listeners. */
public final class DialogueVoiceClientBootstrap {
    private static final DialogueVoiceLifecycleBridge BRIDGE = new DialogueVoiceLifecycleBridge();

    private DialogueVoiceClientBootstrap() {
    }

    public static void bootstrap() {
        MinecraftForge.EVENT_BUS.addListener(BRIDGE::enter);
        MinecraftForge.EVENT_BUS.addListener(BRIDGE::exit);
        MinecraftForge.EVENT_BUS.addListener(BRIDGE::reveal);
        MinecraftForge.EVENT_BUS.addListener(DialogueVoiceClientBootstrap::commands);
    }

    private static void commands(RegisterClientCommandsEvent event) {
        DialogueVoiceClientCommands.register(event, BRIDGE);
    }
}
