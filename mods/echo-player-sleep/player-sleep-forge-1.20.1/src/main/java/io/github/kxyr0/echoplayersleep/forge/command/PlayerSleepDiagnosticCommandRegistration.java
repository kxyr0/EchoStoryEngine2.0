package io.github.kxyr0.echoplayersleep.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echoplayersleep.api.claim.PlayerSleepStoryClaim;
import io.github.kxyr0.echoplayersleep.api.diagnostic.PlayerSleepDiagnostic;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventId;
import io.github.kxyr0.echoplayersleep.api.event.PlayerSleepEventSnapshot;
import io.github.kxyr0.echoplayersleep.api.snapshot.PlayerSleepSessionSnapshot;
import io.github.kxyr0.echoplayersleep.api.world.PlayerRef;
import io.github.kxyr0.echoplayersleep.core.arm.PlayerSleepArmSnapshot;
import io.github.kxyr0.echoplayersleep.forge.service.ForgePlayerSleepServices;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerSleepDiagnosticCommandRegistration {
    private PlayerSleepDiagnosticCommandRegistration() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, ForgePlayerSleepServices services) {
        dispatcher.register(Commands.literal("echo_sleep")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("arm")
                        .then(Commands.literal("status")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> armStatus(context, services)))))
                .then(Commands.literal("session")
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> sessionInspect(context, services)))))
                .then(Commands.literal("event")
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("event_id", StringArgumentType.greedyString())
                                        .executes(context -> eventInspect(context, services)))))
                .then(Commands.literal("claim")
                        .then(Commands.literal("inspect")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> claimInspect(context, services)))))
                .then(Commands.literal("count")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> count(context, services))))
                .then(Commands.literal("diagnostics")
                        .executes(context -> diagnostics(context, services))));
    }

    private static int armStatus(
            CommandContext<CommandSourceStack> context,
            ForgePlayerSleepServices services) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String text = services.activeArm(ref(player)).map(PlayerSleepDiagnosticCommandRegistration::armText)
                .orElse("No active Sleep Arm for player=" + player.getGameProfile().getName());
        send(context, text);
        return 1;
    }

    private static int sessionInspect(
            CommandContext<CommandSourceStack> context,
            ForgePlayerSleepServices services) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String text = services.active(ref(player)).map(PlayerSleepDiagnosticCommandRegistration::sessionText)
                .orElse("No active Sleep Session for player=" + player.getGameProfile().getName());
        send(context, text);
        return 1;
    }

    private static int eventInspect(
            CommandContext<CommandSourceStack> context,
            ForgePlayerSleepServices services) {
        String eventId = StringArgumentType.getString(context, "event_id");
        String text = services.event(new PlayerSleepEventId(eventId))
                .map(PlayerSleepDiagnosticCommandRegistration::eventText)
                .orElse("No Sleep event found for event_id=" + eventId);
        send(context, text);
        return 1;
    }

    private static int claimInspect(
            CommandContext<CommandSourceStack> context,
            ForgePlayerSleepServices services) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String text = services.active(ref(player))
                .flatMap(services::storyClaim)
                .map(PlayerSleepDiagnosticCommandRegistration::claimText)
                .orElse("No active Story claim for player=" + player.getGameProfile().getName());
        send(context, text);
        return 1;
    }

    private static int diagnostics(
            CommandContext<CommandSourceStack> context,
            ForgePlayerSleepServices services) {
        String text = services.diagnostics().isEmpty() ? "No Echo Player Sleep diagnostics."
                : services.diagnostics().stream()
                        .map(PlayerSleepDiagnosticCommandRegistration::diagnosticText)
                        .collect(Collectors.joining(" | "));
        send(context, text);
        return 1;
    }

    private static int count(
            CommandContext<CommandSourceStack> context,
            ForgePlayerSleepServices services) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        send(context, "player=" + player.getGameProfile().getName()
                + " uuid=" + player.getUUID()
                + " sleepCount=" + services.sleepCount(ref(player)));
        return 1;
    }

    private static PlayerRef ref(ServerPlayer player) {
        return new PlayerRef(player.getUUID());
    }

    private static String armText(PlayerSleepArmSnapshot arm) {
        return "arm=" + arm.armId().value()
                + " player=" + arm.player().uuid()
                + " scenario=" + arm.scenarioId().value()
                + " owner=" + arm.owner().value()
                + " revision=" + arm.revision().value()
                + " generation=" + arm.generation()
                + " createdTick=" + arm.createdTick()
                + " consumed=" + arm.consumed();
    }

    private static String sessionText(PlayerSleepSessionSnapshot snapshot) {
        return "session=" + snapshot.sessionId().value()
                + " player=" + snapshot.player().uuid()
                + " scenario=" + snapshot.scenarioId().value()
                + " status=" + snapshot.status()
                + " ownsBedPose=" + snapshot.ownsBedPose()
                + " sleepCount=" + snapshot.sleepCount()
                + " revision=" + snapshot.revision().value()
                + " bed=" + snapshot.bed().canonicalPosition()
                + " dimension=" + snapshot.origin().dimension().value();
    }

    private static String eventText(PlayerSleepEventSnapshot event) {
        return "event=" + event.eventId().value()
                + " kind=" + event.kind()
                + " selector=" + event.fingerprint()
                + " session=" + event.session().sessionId().value()
                + " revision=" + event.session().revision().value()
                + " scenario=" + event.scenarioId().value()
                + " sleepCount=" + event.sleepCount()
                + " tick=" + event.serverTick();
    }

    private static String claimText(PlayerSleepStoryClaim claim) {
        return "session=" + claim.sessionId().value()
                + " subscription=" + claim.triggerSubscriptionId()
                + " triggerExecution=" + claim.triggerExecutionId()
                + " storyExecution=" + claim.storyExecutionId()
                + " owner=" + claim.rootOwner().value()
                + " claimRevision=" + claim.claimRevision().value()
                + " claimedTick=" + claim.claimedServerTick();
    }

    private static String diagnosticText(PlayerSleepDiagnostic diagnostic) {
        return diagnostic.code()
                + " severity=" + diagnostic.severity()
                + " subject=" + diagnostic.subject()
                + " message=" + diagnostic.message();
    }

    private static void send(CommandContext<CommandSourceStack> context, String text) {
        context.getSource().sendSuccess(() -> Component.literal(text), false);
    }
}
