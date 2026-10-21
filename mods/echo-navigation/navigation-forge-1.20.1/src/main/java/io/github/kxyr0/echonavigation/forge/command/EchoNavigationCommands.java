package io.github.kxyr0.echonavigation.forge.command;

import com.mojang.logging.LogUtils;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echonavigation.api.identity.NavigationBackendId;
import io.github.kxyr0.echonavigation.api.identity.NavigationCommandId;
import io.github.kxyr0.echonavigation.api.identity.NavigationExecutionId;
import io.github.kxyr0.echonavigation.api.identity.NavigationMoveId;
import io.github.kxyr0.echonavigation.api.identity.NavigationOwnerId;
import io.github.kxyr0.echonavigation.api.identity.NavigationSubjectId;
import io.github.kxyr0.echonavigation.api.movement.NavigationRequest;
import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.forge.runtime.ForgeNavigationHost;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import org.slf4j.Logger;

public final class EchoNavigationCommands {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Supplier<ForgeNavigationHost> host;

    public EchoNavigationCommands(Supplier<ForgeNavigationHost> host) {
        this.host = host;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_navigation")
                .then(Commands.literal("backend").executes(ctx -> backend(ctx.getSource()))
                        .then(Commands.literal("list").executes(ctx -> backendList(ctx.getSource())))
                        .then(Commands.literal("set")
                                .then(Commands.argument("backend_id", StringArgumentType.string())
                                        .executes(ctx -> setBackend(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "backend_id"))))))
                .then(Commands.literal("move")
                        .then(Commands.argument("npc_id", StringArgumentType.string())
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(ctx -> move(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "npc_id"),
                                                                IntegerArgumentType.getInteger(ctx, "x"),
                                                                IntegerArgumentType.getInteger(ctx, "y"),
                                                                IntegerArgumentType.getInteger(ctx, "z"))))))))
                .then(Commands.literal("stop").then(Commands.argument("npc_id", StringArgumentType.string())
                        .executes(ctx -> stop(ctx.getSource(), StringArgumentType.getString(ctx, "npc_id")))))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("validate").executes(ctx -> reload(ctx.getSource())))
                .then(Commands.literal("diagnostics").executes(ctx -> diagnostics(ctx.getSource()))));
    }

    private int backend(CommandSourceStack source) {
        return withHost(source).map(value -> NavigationCommandText.ok(source,
                "active backend=" + value.service().settings().backendId().value()
                        + ", revision=" + value.service().settings().revision()))
                .orElse(0);
    }

    private int backendList(CommandSourceStack source) {
        return withHost(source).map(value -> NavigationCommandText.ok(source,
                "available backends=" + value.service().backends())).orElse(0);
    }

    private int setBackend(CommandSourceStack source, String backendId) {
        return withHost(source).map(value -> {
            var diagnostics = value.settingsState().setBackend(new NavigationBackendId(backendId),
                    value.backendRegistry().ids());
            diagnostics.forEach(value::diagnostic);
            if (!diagnostics.isEmpty()) {
                return NavigationCommandText.fail(source, diagnostics.get(0).message());
            }
            return NavigationCommandText.ok(source, "backend set to " + backendId);
        }).orElse(0);
    }

    private int move(CommandSourceStack source, String npcId, int x, int y, int z) {
        return withHost(source).map(value -> {
            try {
                String tick = Long.toString(source.getLevel().getGameTime());
                NavigationSubjectId subjectId = new NavigationSubjectId(npcId);
                NavigationDestination destination = destination(source, x, y, z);
                NavigationRequest request = new NavigationRequest(new NavigationMoveId("admin:move_" + tick),
                        new NavigationCommandId("admin:cmd_" + tick), new NavigationExecutionId("admin:execution"),
                        new NavigationOwnerId("admin:command"), subjectId, destination,
                        value.service().settings().backendId(), value.service().settings().revision(), 0L);
                var result = value.service().move(request);
                String message = result.status() + " " + result.message()
                        + result.failure().map(failure -> " " + failure.name()).orElse("");
                return result.status() == io.github.kxyr0.echonavigation.forge.api.service.NavigationService.Status.ACCEPTED
                        ? NavigationCommandText.ok(source, message) : NavigationCommandText.fail(source, message);
            } catch (RuntimeException exception) {
                LOGGER.error("echo_navigation_move_command_failed npcId={} target={}:{}:{}",
                        npcId, x, y, z, exception);
                return NavigationCommandText.fail(source, "NAVIGATION_COMMAND_FAILED "
                        + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }).orElse(0);
    }

    private NavigationDestination destination(CommandSourceStack source, int x, int y, int z) {
        String dimensionId = source.getLevel().dimension().location().toString();
        if (!standable(source.getLevel(), new BlockPos(x, y, z))
                && standable(source.getLevel(), new BlockPos(x, y + 1, z))) {
            return new NavigationDestination(dimensionId, x, y + 1, z);
        }
        return new NavigationDestination(dimensionId, x, y, z);
    }

    private boolean standable(ServerLevel level, BlockPos feet) {
        BlockPos head = feet.above();
        BlockPos floor = feet.below();
        return level.hasChunkAt(feet)
                && !level.getBlockState(feet).getFluidState().is(FluidTags.LAVA)
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(head).getCollisionShape(level, head).isEmpty()
                && !level.getBlockState(floor).getFluidState().is(FluidTags.LAVA)
                && !level.getBlockState(floor).getCollisionShape(level, floor).isEmpty();
    }

    private int stop(CommandSourceStack source, String npcId) {
        return withHost(source).map(value -> {
            var result = value.service().stop(new NavigationSubjectId(npcId));
            return NavigationCommandText.ok(source, result.status() + " " + result.message());
        }).orElse(0);
    }

    private int list(CommandSourceStack source) {
        return withHost(source).map(value -> NavigationCommandText.ok(source,
                "moves=" + value.service().list(20))).orElse(0);
    }

    private int reload(CommandSourceStack source) {
        return withHost(source).map(value -> {
            var diagnostics = value.settingsState().reload(value.backendRegistry().ids());
            diagnostics.forEach(value::diagnostic);
            return diagnostics.isEmpty()
                    ? NavigationCommandText.ok(source, "settings reloaded")
                    : NavigationCommandText.fail(source, diagnostics.get(0).message());
        }).orElse(0);
    }

    private int diagnostics(CommandSourceStack source) {
        return withHost(source).map(value -> NavigationCommandText.ok(source,
                "diagnostics=" + value.service().diagnostics().recent(20))).orElse(0);
    }

    private Optional<ForgeNavigationHost> withHost(CommandSourceStack source) {
        ForgeNavigationHost value = host.get();
        if (value == null) {
            NavigationCommandText.fail(source, "Echo Navigation host is not started.");
            return Optional.empty();
        }
        return Optional.of(value);
    }
}
