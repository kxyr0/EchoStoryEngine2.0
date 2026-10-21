package io.github.kxyr0.echonpcinteraction.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionProviderId;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionOwnerId;
import io.github.kxyr0.echonpcinteraction.forge.api.execution.NpcInteractionExecutionPort;
import io.github.kxyr0.echonpcinteraction.forge.api.provider.NpcInteractionProviderRegistry;
import io.github.kxyr0.echonpcinteraction.forge.inspect.ServerTargetInspector;
import io.github.kxyr0.echonpcinteraction.forge.runtime.ServerInteractionRuntime;
import io.github.kxyr0.echonpcinteraction.forge.seat.NpcSeatOccupancyRegistry;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class EchoNpcInteractionCommands {
    private final Function<MinecraftServer, ServerInteractionRuntime> runtime;
    private final NpcInteractionProviderRegistry providers;
    private final Supplier<List<NpcInteractionExecutionPort>> ports;
    private final NpcSeatOccupancyRegistry occupancies;

    public EchoNpcInteractionCommands(
            Function<MinecraftServer, ServerInteractionRuntime> runtime,
            NpcInteractionProviderRegistry providers,
            Supplier<List<NpcInteractionExecutionPort>> ports,
            NpcSeatOccupancyRegistry occupancies) {
        this.runtime = runtime;
        this.providers = providers;
        this.ports = ports;
        this.occupancies = occupancies;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_npc_interaction")
                .then(Commands.literal("interact").then(Commands.argument("npc_id", StringArgumentType.string())
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(CommandExceptions.guarded(this::interact)))))))
                .then(Commands.literal("stop").then(Commands.argument("npc_id", StringArgumentType.string())
                        .executes(CommandExceptions.guarded(this::stop))))
                .then(Commands.literal("stand_up").then(Commands.argument("npc_id", StringArgumentType.string())
                        .executes(CommandExceptions.guarded(this::standUp))))
                .then(Commands.literal("inspect").then(Commands.argument("npc_id", StringArgumentType.string())
                        .executes(CommandExceptions.guarded(this::inspect))))
                .then(Commands.literal("consequence").then(Commands.argument("npc_id", StringArgumentType.string())
                        .executes(CommandExceptions.guarded(this::inspect))))
                .then(Commands.literal("proxy").then(Commands.literal("status").executes(CommandExceptions.guarded(ctx ->
                        ok(ctx, CommandDebugViews.proxyStatus())))))
                .then(Commands.literal("ride").then(Commands.literal("inspect")
                        .then(Commands.argument("npc_id", StringArgumentType.string())
                                .executes(CommandExceptions.guarded(this::ride)))))
                .then(Commands.literal("seats").executes(CommandExceptions.guarded(ctx ->
                        ok(ctx, CommandDebugViews.seats(occupancies)))))
                .then(Commands.literal("seat").then(Commands.literal("inspect")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(CommandExceptions.guarded(this::seat)))))))
                .then(Commands.literal("target").then(Commands.literal("inspect")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(CommandExceptions.guarded(this::target)))))))
                .then(Commands.literal("providers").executes(CommandExceptions.guarded(this::providers)))
                .then(Commands.literal("provider").then(Commands.literal("inspect")
                        .then(Commands.argument("provider_id", StringArgumentType.string())
                                .executes(CommandExceptions.guarded(this::provider)))))
                .then(Commands.literal("approaches").then(Commands.argument("npc_id", StringArgumentType.string())
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .then(Commands.argument("z", IntegerArgumentType.integer())
                                                .executes(CommandExceptions.guarded(this::approaches)))))))
                .then(Commands.literal("diagnostics").executes(CommandExceptions.guarded(this::diagnostics)))
                .then(Commands.literal("trace").then(Commands.argument("npc_id", StringArgumentType.string())
                        .executes(CommandExceptions.guarded(this::inspect))))
                .then(Commands.literal("trace").then(Commands.literal("off").executes(CommandExceptions.guarded(ctx ->
                        ok(ctx, "Trace disabled."))))));
    }

    private int interact(CommandContext<CommandSourceStack> context) {
        var request = CommandInteractionRequests.interact(context.getSource(), actor(context), position(context));
        return ok(context, CommandFormatting.result(runtime.apply(context.getSource().getServer()).interact(request)));
    }

    private int stop(CommandContext<CommandSourceStack> context) {
        var owner = new NpcInteractionOwnerId("echo_npc_interaction:command");
        return ok(context, CommandFormatting.result(runtime.apply(context.getSource().getServer())
                .stop(actor(context), owner)));
    }

    private int standUp(CommandContext<CommandSourceStack> context) {
        var owner = new NpcInteractionOwnerId("echo_npc_interaction:command");
        return ok(context, CommandFormatting.result(runtime.apply(context.getSource().getServer())
                .standUp(actor(context), owner)));
    }

    private int inspect(CommandContext<CommandSourceStack> context) {
        String text = runtime.apply(context.getSource().getServer()).active(actor(context))
                .map(CommandFormatting::result).orElse("No active interaction.");
        return ok(context, text);
    }

    private int target(CommandContext<CommandSourceStack> context) {
        var request = CommandInteractionRequests.interact(context.getSource(),
                new ActorId("echo_npc_interaction:inspect"), position(context));
        String text = new ServerTargetInspector().inspect(context.getSource().getServer(), request.target())
                .map(view -> "blockId=" + view.snapshot().blockId().value() + " state="
                        + view.snapshot().stateFingerprint()).orElse("Target not found.");
        return ok(context, text);
    }

    private int providers(CommandContext<CommandSourceStack> context) {
        return ok(context, CommandFormatting.providers(providers.providers()));
    }

    private int provider(CommandContext<CommandSourceStack> context) {
        String id = StringArgumentType.getString(context, "provider_id");
        String text = providers.find(new NpcInteractionProviderId(id)).map(CommandFormatting::provider)
                .orElse("Provider not found.");
        return ok(context, text);
    }

    private int approaches(CommandContext<CommandSourceStack> context) {
        var request = CommandInteractionRequests.interact(context.getSource(), actor(context), position(context));
        return ok(context, CommandDebugViews.approaches(context.getSource().getServer(), request, providers, ports));
    }

    private int ride(CommandContext<CommandSourceStack> context) {
        return ok(context, CommandDebugViews.ride(context.getSource().getServer(), actor(context), occupancies));
    }

    private int seat(CommandContext<CommandSourceStack> context) {
        return ok(context, CommandDebugViews.seat(context.getSource().getLevel().dimension().location().toString(),
                position(context), occupancies));
    }

    private int diagnostics(CommandContext<CommandSourceStack> context) {
        String text = runtime.apply(context.getSource().getServer()).diagnostics().diagnostics().toString();
        return ok(context, text);
    }

    private ActorId actor(CommandContext<CommandSourceStack> context) {
        return CommandActorIds.actor(context);
    }

    private BlockPosition position(CommandContext<CommandSourceStack> context) {
        return new BlockPosition(IntegerArgumentType.getInteger(context, "x"),
                IntegerArgumentType.getInteger(context, "y"),
                IntegerArgumentType.getInteger(context, "z"));
    }

    private int ok(CommandContext<CommandSourceStack> context, String message) {
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }
}
