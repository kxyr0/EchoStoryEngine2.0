package io.github.kxyr0.echodialogue.forge.resource;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.forge.diagnostic.DialogueDiagnosticDisplay;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Sends reload failures to online players during authoring.
 */
final class DialogueReloadChatNotifier {
    private static final int MAX_LINES = 8;

    private final DialogueDiagnosticDisplay display = new DialogueDiagnosticDisplay();

    void notifyErrors(Optional<MinecraftServer> server, List<DialogueDiagnostic> diagnostics) {
        List<DialogueDiagnostic> errors = diagnostics.stream()
                .filter(DialogueDiagnostic::isError)
                .toList();
        List<DialogueDiagnostic> actionable = errors.stream()
                .filter(error -> !display.isReloadSummary(error))
                .toList();
        List<DialogueDiagnostic> visibleErrors = actionable.isEmpty() ? errors : actionable;
        List<DialogueDiagnostic> shownErrors = visibleErrors.stream().limit(MAX_LINES).toList();
        if (server.isEmpty() || errors.isEmpty()) {
            return;
        }
        send(server.orElseThrow(), display.reloadFailureHeader(errors.size()), ChatFormatting.RED);
        shownErrors.forEach(error -> send(server.orElseThrow(), display.error(error), ChatFormatting.RED));
        int hiddenErrors = visibleErrors.size() - shownErrors.size();
        if (hiddenErrors > 0) {
            send(server.orElseThrow(), display.hiddenErrors(hiddenErrors), ChatFormatting.RED);
        }
    }

    private void send(MinecraftServer server, String text, ChatFormatting color) {
        server.getPlayerList().getPlayers()
                .forEach(player -> player.sendSystemMessage(Component.literal(text).withStyle(color)));
    }
}
