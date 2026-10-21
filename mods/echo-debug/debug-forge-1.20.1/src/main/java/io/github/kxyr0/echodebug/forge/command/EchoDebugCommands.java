package io.github.kxyr0.echodebug.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echodebug.forge.registry.EchoDebugItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class EchoDebugCommands {
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_debug").requires(source -> source.hasPermission(2))
                .then(Commands.literal("give_trigger_wand")
                        .executes(context -> give(context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> give(EntityArgument.getPlayer(context, "player"))))));
    }

    private int give(ServerPlayer player) {
        player.getInventory().add(new ItemStack(EchoDebugItems.TRIGGER_WAND.get()));
        player.sendSystemMessage(Component.literal("Выдана Палка триггеров."));
        return 1;
    }
}
