package io.github.kxyr0.echoobservation.api.lifecycle;

import io.github.kxyr0.echoobservation.api.identity.ObservationCompletionId;
import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import java.util.Objects;
import java.util.UUID;
import net.minecraftforge.eventbus.api.Event;

public final class ObservationCompletedEvent extends Event {
    private final UUID playerId;
    private final ObservationId observationId;
    private final ObservationCompletionId completionId;
    private final String sourceId;

    public ObservationCompletedEvent(UUID playerId, ObservationId observationId,
            ObservationCompletionId completionId, String sourceId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.observationId = Objects.requireNonNull(observationId, "observationId");
        this.completionId = Objects.requireNonNull(completionId, "completionId");
        this.sourceId = Objects.requireNonNull(sourceId, "sourceId");
    }

    public UUID playerId() { return playerId; }
    public ObservationId observationId() { return observationId; }
    public ObservationResourceId resourceId() { return observationId.resource(); }
    public ObservationCompletionId completionId() { return completionId; }
    public String sourceId() { return sourceId; }
}
