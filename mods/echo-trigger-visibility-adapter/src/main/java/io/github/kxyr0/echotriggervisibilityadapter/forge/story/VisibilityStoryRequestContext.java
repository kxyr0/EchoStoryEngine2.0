package io.github.kxyr0.echotriggervisibilityadapter.forge.story;

import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.forge.api.context.VisibilityStoryContextBridge;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

record VisibilityStoryRequestContext(UUID playerId, VisibilityWatchId watchId) {
    static Optional<VisibilityStoryRequestContext> resolve(
            ActionStartRequest request) {
        Map<String, String> values = VisibilityStoryContextBridge.merge(
                request.executionId().asString(), request.payload());
        Optional<UUID> playerId = playerId(values, request.executionId().asString());
        String watch = values.get("watchId");
        if (playerId.isEmpty() || watch == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new VisibilityStoryRequestContext(
                    playerId.orElseThrow(), new VisibilityWatchId(watch)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    Optional<VisibilityStoryOnlineContext> online(MinecraftServer server) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        return player == null ? Optional.empty()
                : Optional.of(new VisibilityStoryOnlineContext(player, watchId));
    }

    private static Optional<UUID> playerId(Map<String, String> values, String executionId) {
        String explicit = values.get("player");
        if (explicit != null) {
            return parseUuid(explicit);
        }
        String marker = "/player/";
        int start = executionId.indexOf(marker);
        if (start < 0) {
            return Optional.empty();
        }
        int valueStart = start + marker.length();
        int end = executionId.indexOf('/', valueStart);
        String value = end < 0 ? executionId.substring(valueStart) : executionId.substring(valueStart, end);
        return parseUuid(value);
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}

record VisibilityStoryOnlineContext(ServerPlayer player, VisibilityWatchId watchId) {
}
