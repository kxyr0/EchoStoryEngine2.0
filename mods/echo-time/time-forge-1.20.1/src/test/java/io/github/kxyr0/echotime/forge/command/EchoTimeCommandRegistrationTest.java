package io.github.kxyr0.echotime.forge.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

class EchoTimeCommandRegistrationTest {
    @Test
    void setAcceptsColonSeparatedTime() throws CommandSyntaxException {
        assertAcceptsTime(node("set").getChild("time"));
    }

    @Test
    void setDayAcceptsColonSeparatedTime() throws CommandSyntaxException {
        assertAcceptsTime(node("set-day").getChild("day").getChild("time"));
    }

    @Test
    void resetIsExecutableWithoutArguments() {
        assertNotNull(node("reset").getCommand());
    }

    private CommandNode<CommandSourceStack> node(String name) {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        EchoTimeCommandRegistration.register(dispatcher, () -> null, () -> null);
        return dispatcher.getRoot().getChild("echo_time").getChild(name);
    }

    private void assertAcceptsTime(CommandNode<CommandSourceStack> node) throws CommandSyntaxException {
        var argument = (ArgumentCommandNode<CommandSourceStack, ?>) node;
        var type = (StringArgumentType) argument.getType();
        StringReader reader = new StringReader("08:30");
        assertEquals("08:30", type.parse(reader));
        assertFalse(reader.canRead());
    }
}
