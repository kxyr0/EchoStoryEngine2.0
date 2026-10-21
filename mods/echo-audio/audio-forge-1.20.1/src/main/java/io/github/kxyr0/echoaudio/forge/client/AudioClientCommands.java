package io.github.kxyr0.echoaudio.forge.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echoaudio.api.*;
import io.github.kxyr0.echoaudio.forge.network.AudioIntent;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class AudioClientCommands {
    private AudioClientCommands() { }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_audio")
                .then(Commands.literal("list").executes(ctx -> {
                    var catalog = AudioClient.instance().catalog();
                    catalog.problems().forEach(p -> say(ctx.getSource(), p.getMessage()));
                    catalog.status().thenAccept(lines -> net.minecraft.client.Minecraft.getInstance().execute(() ->
                            lines.forEach(line -> say(ctx.getSource(), line)))); return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    AudioClient.instance().status().forEach(line -> say(ctx.getSource(), line)); return 1;
                }))
                .then(Commands.literal("reload").executes(ctx -> {
                    AudioClient.instance().catalog().reload().thenRun(() -> net.minecraft.client.Minecraft.getInstance()
                            .execute(() -> say(ctx.getSource(), "Audio reload complete"))); return 1;
                }))
                .then(Commands.literal("play").then(Commands.argument("id", net.minecraft.commands.arguments.ResourceLocationArgument.id()).executes(ctx -> {
                    var id = AudioPlaybackId.next();
                    AudioClient.instance().play(new AudioIntent(id, "debug", new AudioCue("Play",
                            net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "id").toString(),
                            "", "", new AudioOptions(1, 1, 0, 0, 0)), "", 0, 0, 0));
                    say(ctx.getSource(), id.value().toString()); return 1;
                })))
                .then(Commands.literal("stop").then(Commands.argument("playback", StringArgumentType.word()).executes(ctx -> {
                    AudioClient.instance().stop(new AudioPlaybackId(UUID.fromString(StringArgumentType.getString(ctx, "playback"))), 0);
                    return 1;
                }))));
    }
    private static void say(CommandSourceStack source, String value) { source.sendSuccess(() -> Component.literal(value), false); }
}
