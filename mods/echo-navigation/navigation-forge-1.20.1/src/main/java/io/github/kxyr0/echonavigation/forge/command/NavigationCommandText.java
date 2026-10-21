package io.github.kxyr0.echonavigation.forge.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

final class NavigationCommandText {
    private NavigationCommandText() {
    }

    static int ok(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
