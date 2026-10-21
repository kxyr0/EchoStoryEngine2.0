package io.github.kxyr0.echoitem.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.kxyr0.echoitem.forge.ForgeItemRuntimeState;
import io.github.kxyr0.echoitem.forge.network.CatalogSyncChannel;
import io.github.kxyr0.echoitem.forge.reload.EquippedWeaponRefresher;
import io.github.kxyr0.echoitem.forge.registry.RegisteredWeaponItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;

/**
 * Registers production Echo Item Pack commands.
 */
public final class EchoItemCommands {
    private final EchoItemCommandActions actions;

    public EchoItemCommands(ForgeItemRuntimeState state, RegisteredWeaponItems items,
                            EquippedWeaponRefresher equippedWeapons, CatalogSyncChannel catalogSync) {
        this.actions = new EchoItemCommandActions(state, items, equippedWeapons, catalogSync);
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_item").requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(actions::list))
                .then(Commands.literal("reload").executes(actions::reload))
                .then(Commands.literal("validate").executes(actions::validate))
                .then(Commands.literal("pending").executes(actions::pending))
                .then(Commands.literal("inspect")
                        .then(Commands.argument("item_id", ResourceLocationArgument.id())
                                .executes(actions::inspect)))
                .then(Commands.literal("sources")
                        .then(Commands.argument("item_id", ResourceLocationArgument.id())
                                .executes(actions::sources)))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("item_id", ResourceLocationArgument.id())
                                        .executes(actions::giveOne)
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                                                .executes(actions::give))))));
    }
}
