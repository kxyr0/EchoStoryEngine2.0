package io.github.kxyr0.echoobservation.core.focus;

import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.model.ObservationDefinition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ObservationFocusSessions {
    public static final int HEARTBEAT_TIMEOUT_TICKS = 8;
    private final Map<UUID, ObservationFocusSession> sessions = new LinkedHashMap<>();

    public synchronized ObservationFocusSession start(UUID playerId, ObservationDefinition definition, long tick) {
        ObservationFocusSession session = new ObservationFocusSession(playerId, definition.id(),
                definition.revision(), tick, tick, definition.focus().ticks());
        sessions.put(playerId, session);
        return session;
    }

    public synchronized boolean heartbeat(UUID playerId, ObservationId id, long revision, long tick) {
        ObservationFocusSession current = sessions.get(playerId);
        if (current == null || !current.observationId().equals(id) || current.definitionRevision() != revision) {
            return false;
        }
        sessions.put(playerId, current.heartbeat(tick));
        return true;
    }

    public synchronized Optional<ObservationFocusSession> stop(UUID playerId) {
        return Optional.ofNullable(sessions.remove(playerId));
    }

    public synchronized Optional<ObservationFocusSession> session(UUID playerId) {
        return Optional.ofNullable(sessions.get(playerId));
    }

    public synchronized java.util.List<ObservationFocusSession> completable(long tick) {
        java.util.List<ObservationFocusSession> completed = sessions.values().stream()
                .filter(value -> !value.timedOut(tick, HEARTBEAT_TIMEOUT_TICKS) && value.complete(tick)).toList();
        completed.forEach(value -> sessions.remove(value.playerId()));
        sessions.values().removeIf(value -> value.timedOut(tick, HEARTBEAT_TIMEOUT_TICKS));
        return completed;
    }

    public synchronized int size() { return sessions.size(); }
}
