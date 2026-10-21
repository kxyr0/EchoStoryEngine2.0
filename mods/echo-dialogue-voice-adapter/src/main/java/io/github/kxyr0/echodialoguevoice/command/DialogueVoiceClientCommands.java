package io.github.kxyr0.echodialoguevoice.command;

import io.github.kxyr0.echodialoguevoice.lifecycle.DialogueVoiceLifecycleBridge;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

/** Client diagnostics for the adapter state only. */
public final class DialogueVoiceClientCommands {
    private DialogueVoiceClientCommands() {
    }

    public static void register(RegisterClientCommandsEvent event, DialogueVoiceLifecycleBridge bridge) {
        event.getDispatcher().register(Commands.literal("echo_dialogue_voice")
                .then(Commands.literal("status").executes(context -> {
                    bridge.statusLines().forEach(line -> context.getSource()
                            .sendSuccess(() -> Component.literal(line), false));
                    return bridge.statusLines().size();
                })));
    }
}
