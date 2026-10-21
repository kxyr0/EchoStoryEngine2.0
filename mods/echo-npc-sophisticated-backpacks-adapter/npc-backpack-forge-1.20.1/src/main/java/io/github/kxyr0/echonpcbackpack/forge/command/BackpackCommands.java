package io.github.kxyr0.echonpcbackpack.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackEquipRequest;
import io.github.kxyr0.echonpcbackpack.api.service.NpcBackpackUnequipRequest;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionPolicy;
import io.github.kxyr0.echonpcbackpack.api.world.BackpackBlockLocation;
import io.github.kxyr0.echonpcbackpack.api.world.DimensionId;
import io.github.kxyr0.echonpcbackpack.forge.runtime.BackpackRuntime;
import io.github.kxyr0.echonpcbackpack.forge.network.BackpackNetwork;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class BackpackCommands {
    private final BackpackRuntime runtime;
    private final AtomicLong sequence = new AtomicLong();

    public BackpackCommands(BackpackRuntime runtime) {
        this.runtime = runtime;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("echo_npc_backpack").requires(source -> source.hasPermission(2));
        root.then(positionCommand("equip", true));
        root.then(positionCommand("unequip", false));
        root.then(actorCommand("inspect", context -> runtime.inspect(actor(context)).map(Object::toString).orElse("absent")));
        root.then(actorCommand("transition", context -> runtime.inspect(actor(context))
                .flatMap(view -> view.transition().map(Object::toString)).orElse("none")));
        root.then(actorCommand("payload", context -> runtime.inspect(actor(context))
                .flatMap(view -> view.payload().map(Object::toString)).orElse("none")));
        root.then(Commands.literal("reservations").executes(context -> feedback(context.getSource(),
                runtime.reservations().toString())));
        root.then(Commands.literal("diagnostics").executes(context -> feedback(context.getSource(),
                runtime.diagnostics().toString())));
        root.then(Commands.literal("animation").then(Commands.literal("binding")
                .then(actorCommandNode(context -> "Animation marker policy is selected by the submitting facade."))));
        root.then(Commands.literal("recover").then(actorCommandNode(context -> runtime.recover(actor(context),
                commandId(context.getSource(), "recover")).toString())));
        root.then(Commands.literal("debug").then(Commands.literal("anchors")
                .then(actorCommandNode(context -> debug(context, Optional.of(actor(context))))))
                .then(Commands.literal("off").executes(context -> {
                    if (context.getSource().getPlayer() != null) {
                        BackpackNetwork.debug(context.getSource().getPlayer(), Optional.empty());
                    }
                    return feedback(context.getSource(), "Anchor debug off.");
                })));
        dispatcher.register(root);
    }

    private com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> positionCommand(
            String name, boolean equip) {
        return Commands.literal(name).then(Commands.argument("npc_id", ResourceLocationArgument.id())
                .then(Commands.argument("x", IntegerArgumentType.integer())
                .then(Commands.argument("y", IntegerArgumentType.integer())
                .then(Commands.argument("z", IntegerArgumentType.integer()).executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ActorId actor = actor(context);
                    BackpackBlockLocation location = location(context);
                    NpcBackpackCommandId command = commandId(source, name);
                    Object result = equip
                            ? runtime.equip(new NpcBackpackEquipRequest(actor, location, command,
                            NpcBackpackTransitionPolicy.IMMEDIATE))
                            : runtime.unequip(new NpcBackpackUnequipRequest(actor, location, command,
                            NpcBackpackTransitionPolicy.IMMEDIATE));
                    return feedback(source, result.toString());
                })))));
    }

    private com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack, ?> actorCommand(
            String name, java.util.function.Function<CommandContext<CommandSourceStack>, String> action) {
        return Commands.literal(name).then(actorCommandNode(action));
    }

    private com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> actorCommandNode(
            java.util.function.Function<CommandContext<CommandSourceStack>, String> action) {
        return Commands.argument("npc_id", ResourceLocationArgument.id()).executes(context ->
                feedback(context.getSource(), action.apply(context)));
    }

    private ActorId actor(CommandContext<CommandSourceStack> context) {
        return new ActorId(ResourceLocationArgument.getId(context, "npc_id").toString());
    }

    private BackpackBlockLocation location(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        return new BackpackBlockLocation(new DimensionId(source.getLevel().dimension().location().toString()),
                new BlockPosition(IntegerArgumentType.getInteger(context, "x"),
                        IntegerArgumentType.getInteger(context, "y"),
                        IntegerArgumentType.getInteger(context, "z")));
    }

    private NpcBackpackCommandId commandId(CommandSourceStack source, String operation) {
        String suffix = operation.toLowerCase(Locale.ROOT) + "_" + source.getLevel().getGameTime()
                + "_" + sequence.incrementAndGet();
        return new NpcBackpackCommandId("echo_npc_sophisticated_backpacks:" + suffix);
    }

    private String debug(CommandContext<CommandSourceStack> context, Optional<ActorId> actor) {
        if (context.getSource().getPlayer() == null) return "Anchor debug is client-player only.";
        BackpackNetwork.debug(context.getSource().getPlayer(), actor);
        return "Anchor debug enabled for " + actor.orElseThrow() + ".";
    }

    private int feedback(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
