package io.github.kxyr0.echoobservation.core.focus;

import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import java.util.Objects;
import java.util.UUID;

public record ObservationFocusSession(
        UUID playerId,
        ObservationId observationId,
        long definitionRevision,
        long startedTick,
        long heartbeatTick,
        int requiredTicks) {
    public ObservationFocusSession {
        playerId = Objects.requireNonNull(playerId, "playerId");
        observationId = Objects.requireNonNull(observationId, "observationId");
        if (definitionRevision < 0L || startedTick < 0L || heartbeatTick < startedTick || requiredTicks < 1) {
            throw new IllegalArgumentException("Invalid focus session.");
        }
    }

    public ObservationFocusSession heartbeat(long tick) {
        return new ObservationFocusSession(playerId, observationId, definitionRevision,
                startedTick, Math.max(heartbeatTick, tick), requiredTicks);
    }

    public boolean complete(long tick) { return tick - startedTick >= requiredTicks; }
    public boolean timedOut(long tick, int timeoutTicks) { return tick - heartbeatTick > timeoutTicks; }
}
