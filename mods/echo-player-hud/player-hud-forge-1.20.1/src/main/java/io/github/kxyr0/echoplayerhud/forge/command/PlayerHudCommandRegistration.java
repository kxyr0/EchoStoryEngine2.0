package io.github.kxyr0.echoplayerhud.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.kxyr0.echoplayerhud.api.result.HudDiagnostic;
import io.github.kxyr0.echoplayerhud.forge.config.PlayerHudConfigLoader;
import io.github.kxyr0.echoplayerhud.forge.service.ForgePlayerHudServices;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Production command surface for HUD reload, inspection, metrics, and diagnostics.
 */
public final class PlayerHudCommandRegistration {
    private final ForgePlayerHudServices services;
    private final PlayerHudConfigLoader configLoader;

    public PlayerHudCommandRegistration(ForgePlayerHudServices services, PlayerHudConfigLoader configLoader) {
        this.services = services;
        this.configLoader = configLoader;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("echo_hud").requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(context -> reload(context.getSource())))
                .then(Commands.literal("validate").executes(context -> validate(context.getSource())))
                .then(Commands.literal("inspect").executes(context -> inspect(context.getSource())))
                .then(Commands.literal("metrics").executes(context -> metrics(context.getSource())))
                .then(Commands.literal("diagnostics").executes(context -> diagnostics(context.getSource()))));
    }

    private int reload(CommandSourceStack source) {
        boolean loaded = services.reloadLayout(configLoader.load(), configLoader.configPath().toString());
        success(source, (loaded ? "Echo Player HUD loaded " : "Echo Player HUD rejected ")
                + services.layoutSource() + " diagnostics=" + services.layoutDiagnostics().size());
        return 1;
    }

    private int validate(CommandSourceStack source) {
        success(source, "Echo Player HUD source=" + services.layoutSource()
                + " bars=" + services.layout().bars().size()
                + " revision=" + services.layout().revision()
                + " reference=" + services.layout().referenceWidth() + "x" + services.layout().referenceHeight());
        return 1;
    }

    private int inspect(CommandSourceStack source) {
        success(source, "Echo Player HUD providers=" + services.providers().size()
                + " diagnostics=" + services.diagnostics().size());
        return 1;
    }

    private int metrics(CommandSourceStack source) {
        services.providers().forEach(provider -> success(source, "metric=" + provider.metricId()));
        return 1;
    }

    private int diagnostics(CommandSourceStack source) {
        if (services.diagnostics().isEmpty()) {
            success(source, "No Echo Player HUD diagnostics.");
            return 1;
        }
        for (HudDiagnostic diagnostic : services.diagnostics()) {
            success(source, diagnostic.code() + " " + diagnostic.message());
        }
        return 1;
    }

    private void success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }
}
