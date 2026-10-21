package io.github.kxyr0.echonpcinteraction.forge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

final class CommandExceptions {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CommandExceptions() {
    }

    static Command<CommandSourceStack> guarded(Action action) {
        return context -> {
            try {
                return action.run(context);
            } catch (RuntimeException exception) {
                LOGGER.error("NPC interaction command failed", exception);
                context.getSource().sendFailure(Component.literal("NPC interaction command failed: "
                        + exception.getMessage()));
                return 0;
            }
        };
    }

    @FunctionalInterface
    interface Action {
        int run(CommandContext<CommandSourceStack> context);
    }
}
