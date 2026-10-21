package io.github.kxyr0.echostoryengine.blackspace.story;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class BlackspaceStoryPayload {
    private BlackspaceStoryPayload() {
    }

    static ServerPlayer player(MinecraftServer server, Map<String, String> payload) {
        String uuid = payload.get("playerUuid");
        if (uuid != null && !uuid.isBlank()) {
            ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuid.trim()));
            if (player == null) {
                throw new IllegalArgumentException("playerUuid does not match an online player.");
            }
            return player;
        }
        String name = payload.get("playerName");
        if (name != null && !name.isBlank()) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(name.trim());
            if (player == null) {
                throw new IllegalArgumentException("playerName does not match an online player.");
            }
            return player;
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.size() == 1) {
            return players.get(0);
        }
        throw new IllegalArgumentException("playerUuid or playerName is required when online player count is not 1.");
    }

    static String instanceId(Map<String, String> payload) {
        return optional(payload, "instanceId")
                .or(() -> optional(payload, "id"))
                .or(() -> optional(payload, "target").map(BlackspaceStoryPayload::instanceIdFromTarget))
                .map(BlackspaceStoryPayload::validateId)
                .orElseThrow(() -> new IllegalArgumentException("Missing blackspace instanceId."));
    }

    static Optional<String> optional(Map<String, String> payload, String key) {
        String value = payload.get(key);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    private static String instanceIdFromTarget(String target) {
        if (target.endsWith(".Spawn")) {
            throw new IllegalArgumentException("Blackspace spawn references must be compiled to an instanceId.");
        }
        return target;
    }

    private static String validateId(String id) {
        NamespacedId.of(id);
        return id;
    }
}
