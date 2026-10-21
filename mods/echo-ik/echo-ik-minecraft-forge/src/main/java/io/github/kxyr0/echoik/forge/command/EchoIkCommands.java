package io.github.kxyr0.echoik.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echoik.core.math.Vec3;
import io.github.kxyr0.echoik.forge.network.IkNetworkChannel;
import io.github.kxyr0.echoik.forge.runtime.IkConstraintSnapshot;
import io.github.kxyr0.echoik.forge.runtime.IkRuntimeState;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.navigation.EchoNpcNavigationBodyBridge;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBody;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBodyLookupResult;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;

/**
 * Manual runtime commands for the minimal Echo IK world-target slice.
 */
public final class EchoIkCommands {
    private static final String ARM = "right_arm";

    private final IkRuntimeState state;
    private final IkNetworkChannel network;
    private final NpcTargetSpace targetSpace = new NpcTargetSpace();

    public EchoIkCommands(IkRuntimeState state, IkNetworkChannel network) {
        this.state = Objects.requireNonNull(state, "state");
        this.network = Objects.requireNonNull(network, "network");
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_ik").requires(source -> source.hasPermission(2))
                .then(Commands.literal("target").then(subject().then(arm().then(vec(this::target)))))
                .then(Commands.literal("pole").then(subject().then(arm().then(vec(this::pole)))))
                .then(Commands.literal("weight").then(subject().then(arm()
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0D, 1.0D))
                                .executes(this::weight)))))
                .then(Commands.literal("clear").then(subject().then(arm().executes(this::clear))))
                .then(Commands.literal("debug").then(subject()
                        .then(Commands.literal("on").executes(context -> debug(context, true)))
                        .then(Commands.literal("off").executes(context -> debug(context, false))))));
    }

    private com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> subject() {
        return Commands.argument("npcId", ResourceLocationArgument.id());
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> arm() {
        return Commands.literal(ARM);
    }

    private com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, Double> vec(
            CommandAction action
    ) {
        return Commands.argument("x", DoubleArgumentType.doubleArg())
                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("z", DoubleArgumentType.doubleArg()).executes(action::run)));
    }

    private int target(CommandContext<CommandSourceStack> context) {
        Optional<NpcNavigationBody> body = body(context);
        if (body.isEmpty()) {
            return 0;
        }
        Vec3 local = localTarget(context, body.orElseThrow());
        return publish(context, state.setTarget(subjectId(context), local),
                "Echo IK target updated: " + subjectId(context) + " " + ARM);
    }

    private int pole(CommandContext<CommandSourceStack> context) {
        Optional<NpcNavigationBody> body = body(context);
        if (body.isEmpty()) {
            return 0;
        }
        Vec3 local = localPoint(context, body.orElseThrow());
        Optional<IkConstraintSnapshot> snapshot = state.setPole(subjectId(context), local);
        return snapshot.map(value -> publish(context, value, "Echo IK pole updated: " + subjectId(context)))
                .orElseGet(() -> fail(context, "Set /echo_ik target first for " + subjectId(context)));
    }

    private int weight(CommandContext<CommandSourceStack> context) {
        double value = DoubleArgumentType.getDouble(context, "value");
        Optional<IkConstraintSnapshot> snapshot = state.setWeight(subjectId(context), value);
        return snapshot.map(next -> publish(context, next, "Echo IK weight -> " + value))
                .orElseGet(() -> fail(context, "No active IK target for " + subjectId(context)));
    }

    private int clear(CommandContext<CommandSourceStack> context) {
        Optional<IkConstraintSnapshot> snapshot = state.clear(subjectId(context));
        return snapshot.map(next -> publish(context, next, "Echo IK constraint clearing."))
                .orElseGet(() -> fail(context, "No active IK constraint for " + subjectId(context)));
    }

    private int debug(CommandContext<CommandSourceStack> context, boolean enabled) {
        IkConstraintSnapshot snapshot = state.setDebug(subjectId(context), enabled);
        return publish(context, snapshot, "Echo IK debug " + (enabled ? "enabled." : "disabled."));
    }

    private Optional<NpcNavigationBody> body(CommandContext<CommandSourceStack> context) {
        ActorId actorId = actorId(context);
        NpcNavigationBodyLookupResult lookup =
                EchoNpcNavigationBodyBridge.lookup(context.getSource().getServer(), actorId);
        if (lookup.status() != NpcNavigationBodyLookupResult.Status.RESOLVED || lookup.body().isEmpty()) {
            fail(context, "Echo NPC body unavailable: " + actorId + " (" + lookup.message() + ")");
            return Optional.empty();
        }
        NpcNavigationBody body = lookup.body().orElseThrow();
        if (body.level() != context.getSource().getLevel()) {
            fail(context, "NPC is in another dimension: " + body.dimensionId());
            return Optional.empty();
        }
        return Optional.of(body);
    }

    private ActorId actorId(CommandContext<CommandSourceStack> context) {
        String value = ResourceLocationArgument.getId(context, "npcId").toString();
        if (value.contains("/")) {
            throw new IllegalArgumentException("npcId must not contain '/': " + value);
        }
        return new ActorId(value);
    }

    private String subjectId(CommandContext<CommandSourceStack> context) {
        return actorId(context).value();
    }

    private Vec3 localTarget(CommandContext<CommandSourceStack> context, NpcNavigationBody body) {
        return targetSpace.toLocalStandingTarget(body, DoubleArgumentType.getDouble(context, "x"),
                DoubleArgumentType.getDouble(context, "y"), DoubleArgumentType.getDouble(context, "z"));
    }

    private Vec3 localPoint(CommandContext<CommandSourceStack> context, NpcNavigationBody body) {
        return targetSpace.toLocal(body, DoubleArgumentType.getDouble(context, "x"),
                DoubleArgumentType.getDouble(context, "y"), DoubleArgumentType.getDouble(context, "z"));
    }

    private int publish(CommandContext<CommandSourceStack> context, IkConstraintSnapshot snapshot, String message) {
        network.sendAll(context.getSource().getServer(), snapshot);
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private int fail(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendFailure(Component.literal(message));
        return 0;
    }

    @FunctionalInterface
    private interface CommandAction {
        int run(CommandContext<CommandSourceStack> context);
    }
}
