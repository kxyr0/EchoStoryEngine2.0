package io.github.kxyr0.echoquests.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.github.kxyr0.echoquests.api.identity.QuestEventId;
import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.runtime.QuestService;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

public final class QuestCommands {
    private QuestCommands() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, Supplier<QuestService> services) {
        dispatcher.register(Commands.literal("echo_quests").requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("quest", StringArgumentType.string())
                                        .executes(context -> start(context.getSource(), services.get(),
                                                EntityArgument.getPlayer(context, "player").getUUID(),
                                                StringArgumentType.getString(context, "quest"))))))
                .then(Commands.literal("event")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("quest", StringArgumentType.string())
                                        .then(Commands.argument("event", StringArgumentType.string())
                                                .executes(context -> event(context.getSource(), services.get(),
                                                        EntityArgument.getPlayer(context, "player").getUUID(),
                                                        StringArgumentType.getString(context, "quest"),
                                                        StringArgumentType.getString(context, "event"))))))));
    }

    private static int start(CommandSourceStack source, QuestService service, java.util.UUID player, String quest) {
        var result = service.start(player, QuestId.parse(quest));
        source.sendSuccess(() -> Component.literal(result.detail()), false);
        return result.status().ordinal() == 0 ? 1 : 0;
    }

    private static int event(CommandSourceStack source, QuestService service, java.util.UUID player, String quest,
            String event) {
        var result = service.dispatch(player, QuestId.parse(quest), QuestEventId.parse(event));
        source.sendSuccess(() -> Component.literal(result.detail()), false);
        return result.progress().isPresent() ? 1 : 0;
    }
}
