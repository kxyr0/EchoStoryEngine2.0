package io.github.kxyr0.echoplayersleep.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepCommandId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepOwnerId;
import io.github.kxyr0.echoplayersleep.api.id.PlayerSleepScenarioId;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.forge.service.ForgePlayerSleepServices;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerSleepCommandRegistration {
    private PlayerSleepCommandRegistration() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, ForgePlayerSleepServices services) {
        dispatcher.register(Commands.literal("playersleep")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("arm")
                        .then(Commands.argument("scenario", StringArgumentType.greedyString())
                                .executes(context -> arm(context.getSource(), services,
                                        StringArgumentType.getString(context, "scenario")))))
                .then(Commands.literal("disarm")
                        .executes(context -> disarm(context.getSource(), services)))
                .then(Commands.literal("active")
                        .executes(context -> active(context.getSource(), services)))
                .then(Commands.literal("count")
                        .executes(context -> count(context.getSource(), services))
                        .then(Commands.literal("reset")
                                .executes(context -> resetCount(context.getSource(), services))))
                .then(Commands.literal("clear")
                        .executes(context -> clear(context.getSource(), services))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> clear(context.getSource(), services,
                                        EntityArgument.getPlayer(context, "player")))))
                .then(Commands.literal("diagnostics")
                        .executes(context -> diagnostics(context.getSource(), services))));
    }

    private static int arm(CommandSourceStack source, ForgePlayerSleepServices services, String scenario)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PlayerRef ref = new PlayerRef(player.getUUID());
        var result = services.arm(ref, new PlayerSleepScenarioId(scenario),
                new PlayerSleepOwnerId("command:" + player.getUUID()),
                new PlayerSleepCommandId("command:" + player.serverLevel().getGameTime() + ":" + scenario));
        source.sendSuccess(() -> Component.literal("Echo Player Sleep arm result: " + result.outcome()), false);
        return result.diagnostics().isEmpty() ? 1 : 0;
    }

    private static int disarm(CommandSourceStack source, ForgePlayerSleepServices services)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        boolean removed = services.disarm(new PlayerRef(source.getPlayerOrException().getUUID()));
        source.sendSuccess(() -> Component.literal("Echo Player Sleep disarm: " + removed), false);
        return removed ? 1 : 0;
    }

    private static int active(CommandSourceStack source, ForgePlayerSleepServices services)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerRef player = new PlayerRef(source.getPlayerOrException().getUUID());
        String text = services.active(player).map(PlayerSleepCommandRegistration::sessionText)
                .orElse("No active Echo Player Sleep session.");
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int count(CommandSourceStack source, ForgePlayerSleepServices services)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerRef player = new PlayerRef(source.getPlayerOrException().getUUID());
        source.sendSuccess(() -> Component.literal("Echo Player Sleep count: " + services.sleepCount(player)), false);
        return 1;
    }

    private static int resetCount(CommandSourceStack source, ForgePlayerSleepServices services)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerRef player = new PlayerRef(source.getPlayerOrException().getUUID());
        long previous = services.resetSleepCount(player);
        source.sendSuccess(() -> Component.literal(
                "Echo Player Sleep count reset: " + previous + " -> 0"), false);
        return 1;
    }

    private static int clear(CommandSourceStack source, ForgePlayerSleepServices services)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return clear(source, services, source.getPlayerOrException());
    }

    private static int clear(CommandSourceStack source, ForgePlayerSleepServices services, ServerPlayer player) {
        PlayerRef ref = new PlayerRef(player.getUUID());
        ForgePlayerSleepServices.PlayerSleepClearReport report = services.clearForTesting(ref);
        source.sendSuccess(() -> Component.literal("Echo Player Sleep clear: player="
                + player.getGameProfile().getName()
                + " activeSession=" + report.hadActiveSession()
                + " activeArm=" + report.hadActiveArm()
                + " disarmed=" + report.disarmed()
                + " sessionsRemoved=" + report.sessionsRemoved()
                + " claimsRemoved=" + report.claimsRemoved()
                + " eventsRemoved=" + report.eventsRemoved()
                + " sleepCount=" + report.previousSleepCount() + " -> 0"
                + " diagnosticsRemoved=" + report.diagnosticsRemoved()
                + " clientOverlayReset=" + report.clientOverlayReset()
                + " returnedToBed=" + report.returnedToBed()), false);
        return 1;
    }

    private static int diagnostics(CommandSourceStack source, ForgePlayerSleepServices services) {
        String text = services.diagnostics().isEmpty() ? "No Echo Player Sleep diagnostics."
                : services.diagnostics().stream()
                        .map(diagnostic -> diagnostic.code() + " " + diagnostic.subject())
                        .collect(Collectors.joining(" | "));
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static String sessionText(PlayerSleepSessionSnapshot snapshot) {
        return "session=" + snapshot.sessionId().value()
                + " scenario=" + snapshot.scenarioId().value()
                + " sleepCount=" + snapshot.sleepCount()
                + " ownsBedPose=" + snapshot.ownsBedPose()
                + " status=" + snapshot.status();
    }
}
