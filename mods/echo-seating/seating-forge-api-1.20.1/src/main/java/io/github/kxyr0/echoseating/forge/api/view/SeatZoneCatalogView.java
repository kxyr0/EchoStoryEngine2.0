package io.github.kxyr0.echoseating.forge.api.view;

import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import java.util.List;
import java.util.Optional;

public interface SeatZoneCatalogView {
    long catalogRevision();

    List<SeatZoneDefinition> zones();

    Optional<SeatZoneDefinition> find(SeatZoneId zoneId);
}
