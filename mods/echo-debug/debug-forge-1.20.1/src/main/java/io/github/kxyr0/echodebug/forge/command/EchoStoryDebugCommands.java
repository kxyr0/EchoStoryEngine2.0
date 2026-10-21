package io.github.kxyr0.echodebug.forge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionBridge;
import io.github.kxyr0.echostoryengine.story.extension.forge.EchoStoryExecutionLookupResult;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;

public final class EchoStoryDebugCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_debug").requires(source -> source.hasPermission(2))
                .then(Commands.literal("story")
                        .then(Commands.literal("start")
                                .then(Commands.argument("storyId", ResourceLocationArgument.id())
                                        .executes(context -> start(context, defaultExecutionId(context)))
                                        .then(Commands.argument("executionId", ResourceLocationArgument.id())
                                                .executes(context -> start(context,
                                                        id(context, "executionId"))))))));
    }

    private static int start(CommandContext<CommandSourceStack> context, String rawExecutionId) {
        try {
            StoryId storyId = StoryId.of(id(context, "storyId"));
            ExecutionId executionId = ExecutionId.of(rawExecutionId);
            EchoStoryExecutionLookupResult lookup =
                    EchoStoryExecutionBridge.lookup(context.getSource().getServer());
            if (lookup.status() != EchoStoryExecutionLookupResult.Status.RESOLVED) {
                context.getSource().sendFailure(Component.literal(lookup.message()));
                return 0;
            }
            lookup.starter().orElseThrow().start(storyId, executionId);
            context.getSource().sendSuccess(() -> Component.literal("Started story=" + storyId.asString()
                    + ", executionId=" + executionId.asString()), false);
            return Command.SINGLE_SUCCESS;
        } catch (IllegalArgumentException exception) {
            context.getSource().sendFailure(Component.literal("Invalid story/execution id: "
                    + exception.getMessage()));
            return 0;
        }
    }

    private static String defaultExecutionId(CommandContext<CommandSourceStack> context) {
        String storyId = id(context, "storyId");
        String path = storyId.substring(storyId.indexOf(':') + 1).replace('/', '_');
        return "jdh:manual_" + path + "_" + Long.toString(System.currentTimeMillis(), 36);
    }

    private static String id(CommandContext<CommandSourceStack> context, String name) {
        return ResourceLocationArgument.getId(context, name).toString();
    }
}
