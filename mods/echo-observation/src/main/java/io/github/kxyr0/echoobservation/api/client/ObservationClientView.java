package io.github.kxyr0.echoobservation.api.client;

import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import io.github.kxyr0.echoobservation.api.model.ObservationAnchor;
import io.github.kxyr0.echoobservation.core.catalog.ObservationCatalogSnapshot;
import java.util.Optional;
import java.util.Set;

public interface ObservationClientView {
    ObservationCatalogSnapshot catalog();
    Set<String> activeResourceIds();
    Set<String> completedObservationIds();
    Optional<ObservationId> targeted();
    Optional<ObservationClientScreenPoint> project(ObservationAnchor anchor);
    ObservationClientMetrics metrics();
}
