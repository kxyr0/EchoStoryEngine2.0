package io.github.kxyr0.echoobservation.forge.story;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class ObservationStoryPlayerContext {
    private ObservationStoryPlayerContext() {
    }

    static Optional<UUID> resolve(String executionId, Map<String, String> payload) {
        String explicit = payload.get("player");
        if (explicit != null && !explicit.isBlank()) return parse(explicit);
        String marker = "/player/";
        int start = executionId.indexOf(marker);
        if (start < 0) return Optional.empty();
        int valueStart = start + marker.length();
        int end = executionId.indexOf('/', valueStart);
        return parse(end < 0 ? executionId.substring(valueStart) : executionId.substring(valueStart, end));
    }

    private static Optional<UUID> parse(String value) {
        try { return Optional.of(UUID.fromString(value)); }
        catch (IllegalArgumentException exception) { return Optional.empty(); }
    }
}
