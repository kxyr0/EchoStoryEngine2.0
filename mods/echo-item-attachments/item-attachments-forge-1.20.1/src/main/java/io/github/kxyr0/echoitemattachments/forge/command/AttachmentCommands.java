package io.github.kxyr0.echoitemattachments.forge.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import io.github.kxyr0.echoitemattachments.forge.runtime.ServerAttachmentHost;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class AttachmentCommands {
    private final java.util.function.Supplier<ServerAttachmentHost> host;

    public AttachmentCommands(java.util.function.Supplier<ServerAttachmentHost> host) {
        this.host = host;
    }

    public void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(root("echo_item_attachment"));
        event.getDispatcher().register(root("echo_item_attachments"));
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("debug").executes(context -> debug(context.getSource())))
                .then(Commands.literal("debug").then(Commands.literal("markers")
                        .then(subjectArguments("markers"))))
                .then(Commands.literal("debug").then(Commands.literal("transition")
                        .then(subjectArguments("transition"))))
                .then(Commands.literal("debug").then(Commands.literal("anchors")
                        .then(subjectArguments("anchors"))))
                .then(Commands.literal("debug").then(Commands.literal("off")
                        .executes(context -> off(context.getSource()))))
                .then(Commands.literal("recover").executes(context -> recover(context.getSource())))
                .then(Commands.literal("inspect")
                        .then(subjectArguments("inspect")));
    }

    private com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> subjectArguments(
            String mode) {
        return Commands.argument("kind", StringArgumentType.word()).then(
                Commands.argument("subject", StringArgumentType.greedyString()).executes(context ->
                        inspect(context.getSource(), StringArgumentType.getString(context, "kind"),
                                StringArgumentType.getString(context, "subject"), mode)));
    }

    private int debug(CommandSourceStack source) {
        ServerAttachmentHost current = host.get();
        int count = current == null ? 0 : current.records().size();
        source.sendSuccess(() -> Component.literal("Echo Item Attachments records=" + count), false);
        return count;
    }

    private int recover(CommandSourceStack source) {
        ServerAttachmentHost current = host.get();
        if (current == null) return 0;
        current.recoverNow();
        source.sendSuccess(() -> Component.literal("Recovery audit completed."), false);
        return 1;
    }

    private int inspect(CommandSourceStack source, String kind, String value, String mode) {
        ServerAttachmentHost current = host.get();
        if (current == null) return 0;
        try {
            AttachmentSubjectRef subject = new AttachmentSubjectRef(
                    SubjectKind.valueOf(kind.toUpperCase(java.util.Locale.ROOT)), value);
            AttachmentDebugPrinter printer = new AttachmentDebugPrinter();
            var lines = mode.equals("anchors") ? printer.anchors(current, subject)
                    : printer.inspect(current, subject);
            lines.forEach(line -> source.sendSuccess(() -> Component.literal(line), false));
            return lines.size();
        } catch (IllegalArgumentException exception) {
            source.sendFailure(Component.literal("Invalid kind/subject: " + exception.getMessage()));
            return 0;
        }
    }

    private int off(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Attachment anchor debug disabled."), false);
        return 1;
    }
}
