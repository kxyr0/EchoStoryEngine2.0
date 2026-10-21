package io.github.kxyr0.echoeffects.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echoeffects.forge.ForgeEffectServices;
import io.github.kxyr0.echoeffects.forge.content.ForgeEffectContentLoader;
import java.util.Objects;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;

public final class ForgeEffectCommands {
    private final ForgeEffectCommandActions actions;

    public ForgeEffectCommands(ForgeEffectServices services, ForgeEffectContentLoader loader) {
        this.actions = new ForgeEffectCommandActions(Objects.requireNonNull(services, "services"),
                Objects.requireNonNull(loader, "loader"));
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_effects")
                .then(Commands.literal("reload").executes(actions::reload))
                .then(Commands.literal("definitions").executes(actions::definitions))
                .then(Commands.literal("diagnostics").executes(actions::diagnostics))
                .then(Commands.literal("active")
                        .then(Commands.argument("player", EntityArgument.player()).executes(actions::active)))
                .then(Commands.literal("apply")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("effect", ResourceLocationArgument.id())
                                        .executes(actions::apply))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("effect", ResourceLocationArgument.id())
                                        .executes(actions::remove)))));
    }
}
