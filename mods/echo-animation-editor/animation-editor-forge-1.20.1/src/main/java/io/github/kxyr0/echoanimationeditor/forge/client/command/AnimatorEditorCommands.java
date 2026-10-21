package io.github.kxyr0.echoanimationeditor.forge.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.kxyr0.echoanimationeditor.api.mode.AnimatorEditorMode;
import io.github.kxyr0.echoanimationeditor.forge.client.AnimatorEditorClient;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class AnimatorEditorCommands {
    private AnimatorEditorCommands() {
    }

    public static void register(RegisterClientCommandsEvent event, AnimatorEditorClient client) {
        event.getDispatcher().register(Commands.literal("echo_animator")
                .then(Commands.literal("open").executes(context -> open(client, Optional.empty(), Optional.empty()))
                        .then(Commands.argument("subject", ResourceLocationArgument.id())
                                .executes(context -> open(client, Optional.of(id(context, "subject")),
                                        Optional.empty()))
                                .then(Commands.argument("clip", ResourceLocationArgument.id())
                                        .executes(context -> open(client, Optional.of(id(context, "subject")),
                                                Optional.of(id(context, "clip")))))))
                .then(Commands.literal("close").executes(context -> client.close()))
                .then(Commands.literal("toggle").executes(context -> client.toggle()))
                .then(Commands.literal("save").executes(context -> report(context.getSource(), client.save())))
                .then(Commands.literal("autosave").executes(context ->
                        report(context.getSource(), client.autosave())))
                .then(Commands.literal("hot_reload").executes(context ->
                        report(context.getSource(), client.hotReload())))
                .then(Commands.literal("undo").executes(context -> report(context.getSource(), client.undo())))
                .then(Commands.literal("redo").executes(context -> report(context.getSource(), client.redo())))
                .then(Commands.literal("recover").executes(context ->
                        report(context.getSource(), client.recover())))
                .then(Commands.literal("preview").executes(context ->
                        report(context.getSource(), client.preview())))
                .then(Commands.literal("clear_preview").executes(context ->
                        report(context.getSource(), client.clearPreview())))
                .then(Commands.literal("mode")
                        .then(Commands.literal("body").executes(context ->
                                report(context.getSource(), client.selectMode(AnimatorEditorMode.BODY))))
                        .then(Commands.literal("face").executes(context ->
                                report(context.getSource(), client.selectMode(AnimatorEditorMode.FACE)))))
                .then(selectCommand(client))
                .then(Commands.literal("status").executes(context -> status(context.getSource(), client))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> selectCommand(AnimatorEditorClient client) {
        return Commands.literal("select")
                .then(Commands.literal("bone")
                        .then(Commands.argument("bone", StringArgumentType.string())
                                .suggests((context, builder) -> suggest(client.boneSuggestions(), builder))
                                .executes(context -> report(context.getSource(), client.selectBone(
                                        StringArgumentType.getString(context, "bone"))))))
                .then(Commands.literal("face")
                        .then(Commands.argument("faceElement", StringArgumentType.string())
                                .suggests((context, builder) -> suggest(client.faceElementSuggestions(), builder))
                                .executes(context -> report(context.getSource(), client.selectFaceElement(
                                        StringArgumentType.getString(context, "faceElement"))))));
    }

    private static String id(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, String name) {
        return ResourceLocationArgument.getId(context, name).toString();
    }

    private static int open(AnimatorEditorClient client, Optional<String> subjectId, Optional<String> clipId) {
        return client.open(subjectId, clipId) == 1 ? Command.SINGLE_SUCCESS : 0;
    }

    private static int status(CommandSourceStack source, AnimatorEditorClient client) {
        client.statusLines().forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
        return Command.SINGLE_SUCCESS;
    }

    private static int report(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggest(List<String> values, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(values, builder);
    }
}
