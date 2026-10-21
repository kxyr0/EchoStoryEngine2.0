package io.github.kxyr0.echostoryengine.story.forge.resource;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.StoryDiagnosticDisplay;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Sends reload failures to online players during authoring.
 */
final class StoryReloadChatNotifier {
    private static final int MAX_LINES = 8;

    private final StoryDiagnosticDisplay display = new StoryDiagnosticDisplay();

    void notifyErrors(Optional<MinecraftServer> server, List<StoryDiagnostic> diagnostics) {
        List<StoryDiagnostic> errors = diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)
                .toList();
        List<StoryDiagnostic> actionable = errors.stream()
                .filter(error -> !display.isReloadSummary(error))
                .toList();
        List<StoryDiagnostic> visibleErrors = actionable.isEmpty() ? errors : actionable;
        List<StoryDiagnostic> shownErrors = visibleErrors.stream().limit(MAX_LINES).toList();
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
