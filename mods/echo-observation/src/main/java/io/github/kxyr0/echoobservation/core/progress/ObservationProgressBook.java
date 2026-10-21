package io.github.kxyr0.echoobservation.core.progress;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ObservationProgressBook {
    private final Map<UUID, ObservationPlayerProgress> players = new LinkedHashMap<>();

    public synchronized ObservationPlayerProgress player(UUID playerId) {
        return players.computeIfAbsent(playerId, ignored -> new ObservationPlayerProgress());
    }

    public synchronized int completedCount() {
        return players.values().stream().mapToInt(value -> value.completions().size()).sum();
    }

    public synchronized Map<UUID, ObservationPlayerProgress> entries() {
        return Map.copyOf(players);
    }
}
