package io.github.kxyr0.echothoughtspace.forge.command;

import com.mojang.brigadier.Command;
import io.github.kxyr0.echothoughtspace.forge.client.ThoughtSpaceRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class ThoughtSpaceClientCommands {
    private ThoughtSpaceClientCommands() { }

    public static void register(RegisterClientCommandsEvent event, ThoughtSpaceRuntime runtime) {
        event.getDispatcher().register(Commands.literal("echo_thought_space")
                .then(Commands.literal("status").executes(context -> status(context.getSource(), runtime)))
                .then(Commands.literal("list").executes(context -> list(context.getSource(), runtime)))
                .then(Commands.literal("debug")
                        .then(Commands.literal("on").executes(context -> debug(context.getSource(), runtime, true)))
                        .then(Commands.literal("off").executes(context -> debug(context.getSource(), runtime, false)))));
    }

    private static int status(CommandSourceStack source, ThoughtSpaceRuntime runtime) {
        Minecraft minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        success(source, "Thought Space state=" + runtime.state() + " capture=" + runtime.captureActive()
                + " nodes=" + runtime.nodes().size() + " providers=" + runtime.providerCount()
                + " debug=" + runtime.debug());
        success(source, "Thought Space framebuffer=" + window.getWidth() + "x" + window.getHeight()
                + " gui=" + window.getGuiScaledWidth() + "x" + window.getGuiScaledHeight()
                + " camera=" + format(runtime.camera().center().x()) + ","
                + format(runtime.camera().center().y()) + " zoom=" + format(runtime.camera().zoom()));
        success(source, "Thought Space style revision=" + runtime.styleRevision()
                + " files=" + runtime.styleFileCount() + " circle="
                + format(runtime.style().circleRadius()) + " font="
                + format(runtime.style().nodeFontSize()) + " recipes="
                + runtime.combinations().size() + " unlocked=" + runtime.unlockedCombinationCount());
        return Command.SINGLE_SUCCESS;
    }

    private static int list(CommandSourceStack source, ThoughtSpaceRuntime runtime) {
        runtime.refreshForCommand();
        success(source, "Thought Space nodes (" + runtime.nodes().size() + "):");
        runtime.nodes().forEach(node -> success(source,
                node.snapshot().id() + " = " + node.snapshot().label().value()));
        success(source, "Thought Space combinations (" + runtime.combinations().size() + "):");
        runtime.combinations().stream().sorted(java.util.Comparator.comparing(recipe -> recipe.result().value()))
                .forEach(recipe -> success(source, recipe.inputs().first() + " + " + recipe.inputs().second()
                        + " -> " + recipe.result() + " = " + recipe.label().value()));
        return Command.SINGLE_SUCCESS;
    }

    private static int debug(CommandSourceStack source, ThoughtSpaceRuntime runtime, boolean enabled) {
        runtime.debug(enabled);
        success(source, "Thought Space debug " + (enabled ? "enabled" : "disabled") + ".");
        return Command.SINGLE_SUCCESS;
    }

    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.1f", value); }
    private static void success(CommandSourceStack source, String value) {
        source.sendSuccess(() -> Component.literal(value), false);
    }
}
