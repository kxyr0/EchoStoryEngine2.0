package io.github.kxyr0.echotimehud.forge.client;

import com.mojang.brigadier.Command;
import io.github.kxyr0.echotimehud.forge.config.HudConfigLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class TimeHudClientCommands {
    private TimeHudClientCommands() { }

    public static void register(RegisterClientCommandsEvent event, TimeHudRenderer renderer,
                                TimeHudSnapshotStore snapshots, HudConfigLoader configs) {
        event.getDispatcher().register(Commands.literal("echo_time_hud")
                .then(Commands.literal("status").executes(context -> {
                    String status = "Echo Time HUD enabled=" + configs.current().enabled()
                            + " configRevision=" + configs.current().revision()
                            + " configPath=" + configs.path()
                            + " lastLoadSuccessful=" + configs.lastLoadSuccessful()
                            + " snapshot=" + snapshots.current().map(value -> value.instant().day().value()
                            + "/" + value.instant().timeOfDay().formatHHmm()
                            + " timeRevision=" + value.instant().revision().value()).orElse("missing")
                            + " diagnostics=" + configs.diagnostics().size();
                    context.getSource().sendSuccess(() -> Component.literal(status), false);
                    configs.diagnostics().forEach(value -> context.getSource().sendFailure(Component.literal(
                            value.code() + ": " + value.message())));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("reload").executes(context -> {
                    boolean loaded = configs.reload();
                    context.getSource().sendSuccess(() -> Component.literal(loaded
                            ? "Echo Time HUD config reloaded." : "Echo Time HUD config rejected; LKG retained."), false);
                    return loaded ? Command.SINGLE_SUCCESS : 0;
                }))
                .then(Commands.literal("debug").then(Commands.literal("bounds")
                        .then(Commands.literal("on").executes(context -> debug(context, renderer, true)))
                        .then(Commands.literal("off").executes(context -> debug(context, renderer, false)))
                        .executes(context -> {
                            renderer.debugReport().forEach(line -> context.getSource()
                                    .sendSuccess(() -> Component.literal(line), false));
                            return Command.SINGLE_SUCCESS;
                        }))));
    }

    private static int debug(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context,
                             TimeHudRenderer renderer, boolean enabled) {
        renderer.debugBounds(enabled);
        context.getSource().sendSuccess(() -> Component.literal("Echo Time HUD debug bounds "
                + (enabled ? "enabled" : "disabled") + "."), false);
        if (enabled) renderer.debugReport().forEach(line -> context.getSource()
                .sendSuccess(() -> Component.literal(line), false));
        return Command.SINGLE_SUCCESS;
    }
}

