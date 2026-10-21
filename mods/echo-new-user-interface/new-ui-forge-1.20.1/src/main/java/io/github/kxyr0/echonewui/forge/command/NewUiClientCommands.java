package io.github.kxyr0.echonewui.forge.command;

import com.mojang.brigadier.Command;
import io.github.kxyr0.echonewui.core.layout.JournalBookLayout;
import io.github.kxyr0.echonewui.core.layout.JournalBookLayoutResult;
import io.github.kxyr0.echonewui.forge.client.JournalBookPreviewState;
import io.github.kxyr0.echonewui.forge.config.NewUiClientConfig;
import io.github.kxyr0.echonewui.forge.diagnostic.NewUiDiagnostics;
import io.github.kxyr0.echonewui.forge.viewport.UiViewportSnapshot;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

public final class NewUiClientCommands {
    private NewUiClientCommands() {
    }

    public static void register(RegisterClientCommandsEvent event, JournalBookPreviewState state,
                                NewUiDiagnostics diagnostics, ResourceLocation overlayId) {
        event.getDispatcher().register(Commands.literal("echo_new_ui")
                .then(Commands.literal("debug")
                        .then(Commands.literal("book")
                                .then(Commands.literal("show")
                                        .executes(context -> show(context.getSource(), state)))
                                .then(Commands.literal("hide")
                                        .executes(context -> hide(context.getSource(), state)))
                                .then(Commands.literal("toggle")
                                        .executes(context -> toggle(context.getSource(), state)))
                                .then(Commands.literal("status")
                                        .executes(context -> status(context.getSource(), state,
                                                diagnostics, overlayId))))));
    }

    private static int show(CommandSourceStack source, JournalBookPreviewState state) {
        state.show();
        success(source, "Echo New UI journal book preview shown.");
        return Command.SINGLE_SUCCESS;
    }

    private static int hide(CommandSourceStack source, JournalBookPreviewState state) {
        state.hide();
        success(source, "Echo New UI journal book preview hidden.");
        return Command.SINGLE_SUCCESS;
    }

    private static int toggle(CommandSourceStack source, JournalBookPreviewState state) {
        boolean visible = state.toggle();
        success(source, "Echo New UI journal book preview " + (visible ? "shown." : "hidden."));
        return Command.SINGLE_SUCCESS;
    }

    private static int status(CommandSourceStack source, JournalBookPreviewState state,
                              NewUiDiagnostics diagnostics, ResourceLocation overlayId) {
        success(source, "Echo New UI preview=" + visible(state) + " overlay=" + overlayId);
        success(source, "Echo New UI render visible=" + state.visibleRenderCalls()
                + " hidden=" + state.hiddenRenderCalls());
        currentLayout(diagnostics).ifPresent(result -> reportLayout(source, result));
        success(source, "Echo New UI diagnostics=" + diagnostics.snapshot());
        return Command.SINGLE_SUCCESS;
    }

    private static java.util.Optional<JournalBookLayoutResult> currentLayout(NewUiDiagnostics diagnostics) {
        return UiViewportSnapshot.capture(diagnostics)
                .map(viewport -> new JournalBookLayout().layout(viewport, NewUiClientConfig.layout(diagnostics)));
    }

    private static void reportLayout(CommandSourceStack source, JournalBookLayoutResult result) {
        success(source, "Echo New UI framebuffer=" + result.viewport().framebufferWidth()
                + "x" + result.viewport().framebufferHeight()
                + " gui=" + result.viewport().guiWidth() + "x" + result.viewport().guiHeight()
                + " guiScale=" + result.viewport().guiScale());
        success(source, "Echo New UI physical=" + rect(result.physicalRect())
                + " guiRect=" + rect(result.guiRect())
                + " scale=" + String.format(java.util.Locale.ROOT, "%.6f", result.scale())
                + " centerError=" + centerError(result));
    }

    private static String visible(JournalBookPreviewState state) {
        return state.visible() ? "visible" : "hidden";
    }

    private static String rect(io.github.kxyr0.echonewui.core.layout.UiRectangle rect) {
        return rect.x() + "," + rect.y() + "," + rect.width() + "," + rect.height();
    }

    private static String centerError(JournalBookLayoutResult result) {
        return String.format(java.util.Locale.ROOT, "%.3f,%.3f",
                result.centerErrorX(), result.centerErrorY());
    }

    private static void success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }
}
