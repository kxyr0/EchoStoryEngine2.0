package io.github.kxyr0.echoobservation.api.runtime;

import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.identity.ObservationResourceId;
import java.util.UUID;

public interface ObservationService {
    ObservationServiceResult activate(UUID playerId, ObservationResourceId resourceId);
    ObservationServiceResult deactivate(UUID playerId, ObservationResourceId resourceId);
    ObservationServiceResult enable(UUID playerId, ObservationId observationId);
    ObservationServiceResult disable(UUID playerId, ObservationId observationId);
    boolean isActive(UUID playerId, ObservationResourceId resourceId);
    boolean isCompleted(UUID playerId, ObservationId observationId);
}
