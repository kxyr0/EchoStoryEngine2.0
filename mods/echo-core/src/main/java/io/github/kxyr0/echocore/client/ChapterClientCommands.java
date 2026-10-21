package io.github.kxyr0.echocore.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echocore.api.ChapterBackend;
import java.util.concurrent.CompletionStage;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class ChapterClientCommands {
    private ChapterClientCommands() { }
    public static void register(RegisterClientCommandsEvent event, ChapterBackend backend) {
        var chapter = Commands.argument("id", StringArgumentType.word()).suggests((context, builder) -> {
            backend.chapters().forEach(builder::suggest); return builder.buildFuture();
        });
        chapter.then(Commands.literal("status").executes(context -> {
            var snapshot = backend.snapshot(StringArgumentType.getString(context, "id"));
            context.getSource().sendSuccess(() -> Component.literal(snapshot.toString()), false); return 1;
        }));
        for (String action : new String[] {"start", "continue", "restart"}) {
            chapter.then(Commands.literal(action).executes(context -> {
                String id = StringArgumentType.getString(context, "id");
                CompletionStage<io.github.kxyr0.echocore.api.ChapterResult> result = switch (action) {
                    case "start" -> backend.start(id); case "restart" -> backend.restart(id); default -> backend.continueChapter(id);
                };
                result.thenAccept(value -> {
                    if (!value.success()) context.getSource().sendFailure(Component.literal(value.code() + ": " + value.detail()));
                });
                return 1;
            }));
        }
        event.getDispatcher().register(Commands.literal("echo_core").then(Commands.literal("chapter").then(chapter)));
    }
}
