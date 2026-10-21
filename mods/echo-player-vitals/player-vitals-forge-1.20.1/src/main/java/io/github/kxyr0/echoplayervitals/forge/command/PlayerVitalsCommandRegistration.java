package io.github.kxyr0.echoplayervitals.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kxyr0.echoplayervitals.api.mutation.PlayerVitalsMutationId;
import io.github.kxyr0.echoplayervitals.api.result.PlayerVitalsDiagnostic;
import io.github.kxyr0.echoplayervitals.forge.api.lifecycle.PlayerVitalsLifecycleReason;
import io.github.kxyr0.echoplayervitals.forge.service.ForgePlayerVitalsServices;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Production command surface for Vitals inspection, mutation, and diagnostics.
 */
public final class PlayerVitalsCommandRegistration {
    private final ForgePlayerVitalsServices services;
    private final PlayerVitalsCommandText text = new PlayerVitalsCommandText();

    public PlayerVitalsCommandRegistration(ForgePlayerVitalsServices services) {
        this.services = services;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_vitals").requires(source -> source.hasPermission(2))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> inspect(context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("fatigue")
                        .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> inspect(context.getSource(),
                                        EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(context -> set(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "value"))))))
                        .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(context -> add(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "value"))))))
                        .then(Commands.literal("reduce").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(context -> reduce(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"),
                                                IntegerArgumentType.getInteger(context, "value"))))))
                        .then(Commands.literal("restore").then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> restore(context.getSource(),
                                        EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("hunger").then(Commands.literal("status")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> inspect(context.getSource(),
                                        EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("diagnostics").executes(context -> diagnostics(context.getSource()))));
    }

    private int inspect(CommandSourceStack source, ServerPlayer player) {
        services.ensure(player, PlayerVitalsLifecycleReason.RECONNECT);
        source.sendSuccess(() -> Component.literal(text.inspect(services.snapshotFor(player))), false);
        return 1;
    }

    private int set(CommandSourceStack source, ServerPlayer player, int value) {
        services.ensure(player, PlayerVitalsLifecycleReason.RECONNECT);
        services.setFatigue(player.getUUID(), mutationId("set"), value);
        return inspect(source, player);
    }

    private int add(CommandSourceStack source, ServerPlayer player, int value) {
        services.ensure(player, PlayerVitalsLifecycleReason.RECONNECT);
        services.addFatigue(player.getUUID(), mutationId("add"), value);
        return inspect(source, player);
    }

    private int reduce(CommandSourceStack source, ServerPlayer player, int value) {
        services.ensure(player, PlayerVitalsLifecycleReason.RECONNECT);
        services.reduceFatigue(player.getUUID(), mutationId("reduce"), value);
        return inspect(source, player);
    }

    private int restore(CommandSourceStack source, ServerPlayer player) {
        services.ensure(player, PlayerVitalsLifecycleReason.RECONNECT);
        services.restoreFatigue(player.getUUID(), mutationId("restore"));
        return inspect(source, player);
    }

    private int diagnostics(CommandSourceStack source) throws CommandSyntaxException {
        if (services.diagnostics().isEmpty()) {
            source.sendSuccess(() -> Component.literal("No Echo Player Vitals diagnostics."), false);
            return 1;
        }
        for (PlayerVitalsDiagnostic diagnostic : services.diagnostics()) {
            source.sendSuccess(() -> Component.literal(diagnostic.code() + " " + diagnostic.message()), false);
        }
        return 1;
    }

    private PlayerVitalsMutationId mutationId(String action) {
        return new PlayerVitalsMutationId("command:" + action + ":" + UUID.randomUUID());
    }
}
