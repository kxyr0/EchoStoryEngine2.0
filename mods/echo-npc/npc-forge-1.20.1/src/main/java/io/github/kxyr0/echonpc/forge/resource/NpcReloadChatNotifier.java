package io.github.kxyr0.echonpc.forge.resource;

import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * Sends concise reload failures to online players during authoring.
 */
final class NpcReloadChatNotifier {
    private static final int MAX_LINES = 5;

    void notifyErrors(Optional<MinecraftServer> server, List<NpcDiagnostic> diagnostics) {
        List<NpcDiagnostic> errors = diagnostics.stream()
                .filter(NpcDiagnostic::error)
                .limit(MAX_LINES)
                .toList();
        if (server.isEmpty() || errors.isEmpty()) {
            return;
        }
        send(server.orElseThrow(), "Echo NPC reload: ошибка, каталог не обновлён.");
        errors.forEach(error -> send(server.orElseThrow(), "- " + error.code() + ": " + error.message()));
    }

    private void send(MinecraftServer server, String text) {
        server.getPlayerList().getPlayers()
                .forEach(player -> player.sendSystemMessage(Component.literal(text)));
    }
}
