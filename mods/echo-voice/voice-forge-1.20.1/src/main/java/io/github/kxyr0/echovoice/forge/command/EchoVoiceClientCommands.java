package io.github.kxyr0.echovoice.forge.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackOwner;
import io.github.kxyr0.echovoice.api.playback.VoicePlaybackRequest;
import io.github.kxyr0.echovoice.api.playback.VoiceStopReason;
import io.github.kxyr0.echovoice.forge.client.VoiceClientRuntime;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

/** Client-only operational commands for the production asset pipeline. */
public final class EchoVoiceClientCommands {
    private static final AtomicLong DEBUG_GENERATION = new AtomicLong();

    private EchoVoiceClientCommands() {
    }

    public static void register(RegisterClientCommandsEvent event, VoiceClientRuntime runtime) {
        event.getDispatcher().register(Commands.literal("echo_voice")
                .then(Commands.literal("status").executes(context -> lines(context.getSource(),
                        VoiceCommandLines.status(runtime))))
                .then(Commands.literal("stop").executes(context -> lines(context.getSource(),
                        java.util.List.of(runtime.service().stop(VoiceStopReason.EXPLICIT_STOP).detail()))))
                .then(Commands.literal("reload").executes(context -> reload(context.getSource(), runtime)))
                .then(Commands.literal("assets").executes(context -> lines(context.getSource(),
                        VoiceCommandLines.assets(runtime.query()))))
                .then(Commands.literal("asset")
                        .then(Commands.argument("dialogue_id", StringArgumentType.word())
                                .then(Commands.argument("file_stem", StringArgumentType.word())
                                        .executes(context -> asset(context.getSource(), runtime,
                                                StringArgumentType.getString(context, "dialogue_id"),
                                                StringArgumentType.getString(context, "file_stem"))))))
                .then(Commands.literal("play")
                        .then(Commands.argument("dialogue_id", StringArgumentType.word())
                                .then(Commands.argument("file_stem", StringArgumentType.word())
                                        .executes(context -> play(context.getSource(), runtime,
                                                StringArgumentType.getString(context, "dialogue_id"),
                                                StringArgumentType.getString(context, "file_stem"))))))
                .then(Commands.literal("cache").executes(context -> lines(context.getSource(),
                        VoiceCommandLines.cache(runtime))))
                .then(Commands.literal("diagnostics").executes(context -> lines(context.getSource(),
                        VoiceCommandLines.diagnostics(runtime.query())))));
    }

    private static int reload(net.minecraft.commands.CommandSourceStack source, VoiceClientRuntime runtime) {
        runtime.reloadAll();
        return lines(source, java.util.List.of("Echo Voice reload queued."));
    }

    private static int asset(net.minecraft.commands.CommandSourceStack source, VoiceClientRuntime runtime,
            String dialogueId, String stem) {
        try {
            return lines(source, VoiceCommandLines.asset(runtime.query(), key(dialogueId, stem)));
        } catch (IllegalArgumentException exception) {
            return error(source, exception.getMessage());
        }
    }

    private static int play(net.minecraft.commands.CommandSourceStack source, VoiceClientRuntime runtime,
            String dialogueId, String stem) {
        try {
            VoicePlaybackRequest request = new VoicePlaybackRequest(new DialogueSessionId("echo_voice:debug"), 0,
                    DEBUG_GENERATION.incrementAndGet(), key(dialogueId, stem), 1.0, VoicePlaybackOwner.DEBUG);
            return lines(source, java.util.List.of(runtime.service().play(request).detail()));
        } catch (IllegalArgumentException exception) {
            return error(source, exception.getMessage());
        }
    }

    private static VoiceAssetKey key(String dialogueId, String stem) {
        return VoiceAssetKey.of(DialogueId.parse(dialogueId), stem);
    }

    private static int lines(net.minecraft.commands.CommandSourceStack source, java.util.List<String> messages) {
        messages.forEach(message -> source.sendSuccess(() -> Component.literal(message), false));
        return messages.size();
    }

    private static int error(net.minecraft.commands.CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
