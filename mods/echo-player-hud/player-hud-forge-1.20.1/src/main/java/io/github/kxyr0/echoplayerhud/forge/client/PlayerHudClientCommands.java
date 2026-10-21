package io.github.kxyr0.echoplayerhud.forge.client;

import com.mojang.brigadier.Command;
import io.github.kxyr0.echoplayerhud.api.metric.HudMetricSnapshot;
import io.github.kxyr0.echoplayerhud.forge.client.render.PlayerHudRenderer;
import io.github.kxyr0.echoplayerhud.forge.config.PlayerHudConfigLoader;
import io.github.kxyr0.echoplayerhud.forge.service.ForgePlayerHudServices;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

/**
 * Client debug commands for framebuffer bounds proof.
 */
public final class PlayerHudClientCommands {
    private PlayerHudClientCommands() {
    }

    public static void register(RegisterClientCommandsEvent event, PlayerHudRenderer renderer,
                                ForgePlayerHudServices services, PlayerHudConfigLoader configLoader) {
        event.getDispatcher().register(Commands.literal("echo_hud")
                .then(Commands.literal("reload").executes(context -> {
                    boolean loaded = services.reloadLayout(configLoader.load(), configLoader.configPath().toString());
                    renderer.clearIconCache();
                    context.getSource().sendSuccess(() -> Component.literal(status(services, loaded)), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("validate").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(layout(services)), false);
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("bounds").executes(context -> {
                    renderer.debugBounds().forEach(line -> context.getSource()
                            .sendSuccess(() -> Component.literal(line), false));
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("client_metrics").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(
                            "providers=" + services.providers().size()
                                    + " diagnostics=" + services.diagnostics().size()), false);
                    services.providers().forEach(provider -> {
                        HudMetricSnapshot snapshot = provider.snapshot();
                        context.getSource().sendSuccess(() -> Component.literal(
                                "metric=" + provider.metricId()
                                        + " available=" + snapshot.available()
                                        + " current=" + snapshot.current()
                                        + " maximum=" + snapshot.maximum()
                                        + " normalized=" + snapshot.normalized()
                                        + " revision=" + snapshot.revision().value()), false);
                    });
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private static String status(ForgePlayerHudServices services, boolean loaded) {
        return (loaded ? "Echo Player HUD loaded " : "Echo Player HUD rejected ")
                + services.layoutSource() + " diagnostics=" + services.layoutDiagnostics().size();
    }

    private static String layout(ForgePlayerHudServices services) {
        return "Echo Player HUD source=" + services.layoutSource()
                + " bars=" + services.layout().bars().size()
                + " revision=" + services.layout().revision()
                + " reference=" + services.layout().referenceWidth() + "x"
                + services.layout().referenceHeight()
                + " scaleWithResolution=" + services.layout().scaleWithResolution()
                + " diagnostics=" + services.layoutDiagnostics().size();
    }
}
