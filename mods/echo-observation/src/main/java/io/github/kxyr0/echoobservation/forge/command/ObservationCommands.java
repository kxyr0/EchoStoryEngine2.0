package io.github.kxyr0.echoobservation.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.forge.runtime.ObservationServerRuntime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public final class ObservationCommands {
    private ObservationCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_observation")
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("active").executes(context -> active(context.getSource())))
                .then(Commands.literal("inspect").then(Commands.argument("id", StringArgumentType.string())
                        .executes(context -> inspect(context.getSource(), StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("reload").requires(source -> source.hasPermission(2))
                        .executes(context -> reload(context.getSource())))
                .then(Commands.literal("reset").requires(ObservationCommands::mayReset)
                        .then(Commands.argument("id", StringArgumentType.string()).executes(context ->
                                reset(context.getSource(), StringArgumentType.getString(context, "id")))))
                .then(Commands.literal("debug").then(Commands.literal("on").executes(context ->
                        message(context.getSource(), "Observation debug is client-authoring controlled.")))
                        .then(Commands.literal("off").executes(context ->
                                message(context.getSource(), "Observation debug is disabled.")))));
    }

    private static int status(CommandSourceStack source) {
        var runtime = ObservationServerRuntime.instance();
        return message(source, "Observation: resources=" + runtime.catalog().resources().size()
                + " observations=" + runtime.catalog().observationCount()
                + " focusSessions=" + runtime.focusSessionCount()
                + " revision=" + runtime.catalog().revision());
    }

    private static int active(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            return message(source, "Active Observation resources: "
                    + ObservationServerRuntime.instance().progress(player.getUUID()).activeResources());
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) {
            return message(source, "A player command source is required.");
        }
    }

    private static int list(CommandSourceStack source) {
        var ids = ObservationServerRuntime.instance().catalog().resources().stream()
                .flatMap(resource -> resource.observations().stream())
                .map(definition -> definition.id().toString())
                .sorted()
                .toList();
        if (ids.isEmpty()) return message(source, "Observation catalog is empty.");
        ids.forEach(id -> source.sendSuccess(() -> Component.literal("Observation: " + id), false));
        return ids.size();
    }

    private static int inspect(CommandSourceStack source, String rawId) {
        try {
            ObservationId id = ObservationId.parse(rawId);
            var definition = ObservationServerRuntime.instance().catalog().observation(id);
            if (definition.isEmpty()) return message(source, "OBSERVATION_NOT_FOUND: " + id);
            var value = definition.orElseThrow();
            return message(source, value.id() + " source=" + value.source().sourceId() + ":" + value.source().line()
                    + " revision=" + value.revision() + " placed=" + value.placed()
                    + " focusTicks=" + value.focus().ticks() + " anchor=" + value.anchor());
        } catch (IllegalArgumentException exception) {
            return message(source, "OBSERVATION_NOT_FOUND: " + rawId);
        }
    }

    private static int reload(CommandSourceStack source) {
        source.getServer().reloadResources(source.getServer().getPackRepository().getSelectedIds());
        return message(source, "Observation resource reload requested.");
    }

    private static int reset(CommandSourceStack source, String rawId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ObservationId id = ObservationId.parse(rawId);
            boolean changed = ObservationServerRuntime.instance().reset(player.getUUID(), id);
            return message(source, changed ? "Observation completion reset: " + id
                    : "Observation was not completed: " + id);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException | IllegalArgumentException exception) {
            return message(source, "OBSERVATION_NOT_FOUND: " + rawId);
        }
    }

    private static boolean mayReset(CommandSourceStack source) {
        return source.hasPermission(2) || !source.getServer().isDedicatedServer()
                && ModList.get().isLoaded("echo_observation_authoring");
    }

    private static int message(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
