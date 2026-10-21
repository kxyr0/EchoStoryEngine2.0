package io.github.kxyr0.echoplayeractor.story.bridge;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

final class PlayerActorStoryPayload {
    private PlayerActorStoryPayload() {
    }

    static PlayerActorId actorId(MinecraftServer server, Map<String, String> payload) {
        String uuid = payload.get("playerUuid");
        if (uuid != null && !uuid.isBlank()) {
            return PlayerActorId.of(UUID.fromString(uuid));
        }
        String name = payload.get("playerName");
        if (name != null && !name.isBlank()) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(name);
            if (player == null) {
                throw new IllegalArgumentException("playerName does not match an online player.");
            }
            return PlayerActorId.of(player.getUUID());
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.size() == 1) {
            return PlayerActorId.of(players.get(0).getUUID());
        }
        throw new IllegalArgumentException("playerUuid or playerName is required when online player count is not 1.");
    }

    static double number(Map<String, String> payload, String key) {
        return Double.parseDouble(required(payload, key));
    }

    static int integer(Map<String, String> payload, String key) {
        return Integer.parseInt(required(payload, key));
    }

    static int integer(Map<String, String> payload, String key, int fallback) {
        String value = payload.get(key);
        return value == null || value.isBlank() ? fallback : Integer.parseInt(value);
    }

    static boolean bool(Map<String, String> payload, String key, boolean fallback) {
        String value = payload.get(key);
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value);
    }

    static String required(Map<String, String> payload, String key) {
        String value = payload.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing action argument: " + key);
        }
        return value;
    }
}
