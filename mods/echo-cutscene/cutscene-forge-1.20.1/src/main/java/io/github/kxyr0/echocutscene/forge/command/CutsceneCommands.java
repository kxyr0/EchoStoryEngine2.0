package io.github.kxyr0.echocutscene.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import io.github.kxyr0.echocutscene.forge.authoring.ServerCutsceneAuthoringHost;
import io.github.kxyr0.echocutscene.forge.playback.CutsceneResourceLoader;
import io.github.kxyr0.echocutscene.forge.playback.FileSystemTrackWatcher;
import io.github.kxyr0.echocutscene.forge.playback.ServerCutscenePlaybackHost;
import io.github.kxyr0.echocutscene.forge.playback.ServerCutsceneResourceCatalog;
import java.io.IOException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CutsceneCommands {
    private final ServerCutscenePlaybackHost host;
    private final ServerCutsceneResourceCatalog catalog;
    private final CutsceneResourceLoader loader;
    private final FileSystemTrackWatcher watcher;
    private final ServerCutsceneAuthoringHost authoring;

    public CutsceneCommands(ServerCutscenePlaybackHost host, ServerCutsceneResourceCatalog catalog,
            CutsceneResourceLoader loader, FileSystemTrackWatcher watcher, ServerCutsceneAuthoringHost authoring) {
        this.host = host;
        this.catalog = catalog;
        this.loader = loader;
        this.watcher = watcher;
        this.authoring = authoring;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_cutscene").requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("play").then(Commands.argument("id", ResourceLocationArgument.id())
                        .executes(context -> play(context.getSource(),
                                ResourceLocationArgument.getId(context, "id").toString(),
                                CutsceneEndBehavior.AUTO_STOP))
                        .then(Commands.literal("hold").executes(context -> play(context.getSource(),
                                ResourceLocationArgument.getId(context, "id").toString(),
                                CutsceneEndBehavior.HOLD_LAST_FRAME)))))
                .then(Commands.literal("stop").executes(context -> stopActive(context.getSource()))
                        .then(Commands.argument("id", ResourceLocationArgument.id()).executes(context ->
                                stop(context.getSource(), ResourceLocationArgument.getId(context, "id").toString()))))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("reload").executes(context -> reload(context.getSource()))));
        new CutsceneAuthoringCommands(authoring).register(dispatcher);
    }

    private int play(CommandSourceStack source, String id, CutsceneEndBehavior endBehavior)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return report(source, host.start(player, CutsceneId.parse(id), CutsceneCommandId.create(), endBehavior));
    }

    private int list(CommandSourceStack source) {
        var definitions = catalog.definitions();
        if (definitions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Cutscene catalog is empty."), false);
            return 1;
        }
        definitions.forEach(definition -> source.sendSuccess(() -> Component.literal(
                "Cutscene: " + definition.cutsceneId() + " track=" + definition.cameraTrackId()), false));
        return definitions.size();
    }

    private int stopActive(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return report(source, host.stopActive(player, CutsceneCommandId.create()));
    }

    private int stop(CommandSourceStack source, String id)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return report(source, host.stop(source.getPlayerOrException(), CutsceneId.parse(id),
                CutsceneCommandId.create()));
    }

    private int status(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        host.view(player).ifPresentOrElse(view -> source.sendSuccess(() -> Component.literal(
                "cutscene=" + view.cutsceneId() + " prepared=" + view.prepared()
                        + " elapsed=" + view.elapsedMillis()), false),
                () -> source.sendSuccess(() -> Component.literal("cutscene=none"), false));
        return 1;
    }

    private int reload(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        try {
            CutsceneResourceLoader.LoadedCutscenes loaded = loader.load(watcher.root(server));
            catalog.replaceAll(loaded.definitions(), loaded.tracks());
            source.sendSuccess(() -> Component.literal("cutscene reload ok: tracks=" + loaded.tracks().size()), true);
            return 1;
        } catch (IOException | IllegalArgumentException exception) {
            source.sendFailure(Component.literal("cutscene reload failed: " + exception.getMessage()));
            return 0;
        }
    }

    private static int report(CommandSourceStack source, CutsceneResult result) {
        source.sendSuccess(() -> Component.literal("cutscene outcome=" + result.outcome()
                + " playback=" + result.playbackId().map(Object::toString).orElse("none")), false);
        return result.diagnostics().isEmpty() ? 1 : 0;
    }
}
