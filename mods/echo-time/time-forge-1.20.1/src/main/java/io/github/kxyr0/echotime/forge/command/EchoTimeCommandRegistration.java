package io.github.kxyr0.echotime.forge.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echotime.forge.api.EchoTimeServiceBridge;
import io.github.kxyr0.echotime.forge.host.ForgeEchoTimeHost;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;

public final class EchoTimeCommandRegistration {
    private EchoTimeCommandRegistration() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            Supplier<ForgeEchoTimeHost> host, Supplier<EchoTimeServiceBridge> services) {
        dispatcher.register(literal("echo_time")
                .requires(source -> source.hasPermission(2))
                .then(literal("status").executes(context -> actions(context.getSource(), host, services).status()))
                .then(literal("set").then(argument("time", StringArgumentType.greedyString())
                        .executes(context -> actions(context.getSource(), host, services)
                                .set(StringArgumentType.getString(context, "time")))))
                .then(literal("set-day")
                        .then(argument("day", IntegerArgumentType.integer(1))
                                .then(argument("time", StringArgumentType.greedyString())
                                        .executes(context -> actions(context.getSource(), host, services).setDay(
                                                IntegerArgumentType.getInteger(context, "day"),
                                                StringArgumentType.getString(context, "time"))))))
                .then(literal("reset").executes(context -> actions(context.getSource(), host, services).reset()))
                .then(literal("add").then(argument("duration", StringArgumentType.word())
                        .executes(context -> actions(context.getSource(), host, services)
                                .add(StringArgumentType.getString(context, "duration")))))
                .then(literal("pause").executes(context -> actions(context.getSource(), host, services).pause()))
                .then(literal("resume").executes(context -> actions(context.getSource(), host, services).resume()))
                .then(literal("pauses").executes(context -> actions(context.getSource(), host, services).pauses()))
                .then(literal("speed").executes(context -> actions(context.getSource(), host, services).speed()))
                .then(literal("reload").executes(context -> actions(context.getSource(), host, services).reload()))
                .then(literal("resync").executes(context -> actions(context.getSource(), host, services).resync()))
                .then(literal("next").executes(context -> actions(context.getSource(), host, services).next()))
                .then(literal("diagnostics")
                        .executes(context -> actions(context.getSource(), host, services).diagnostics())));
    }

    private static EchoTimeCommandActions actions(CommandSourceStack source,
            Supplier<ForgeEchoTimeHost> host, Supplier<EchoTimeServiceBridge> services) {
        return new EchoTimeCommandActions(source, host.get(), services.get());
    }
}
