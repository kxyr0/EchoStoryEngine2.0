package io.github.kxyr0.echoseating.core.lifecycle;

import io.github.kxyr0.echoseating.api.anchor.SeatAnchor;
import io.github.kxyr0.echoseating.api.id.SeatSlotId;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import java.util.Objects;
import java.util.Optional;

public record SeatResolution(
        SeatZoneDefinition zone,
        Optional<SeatSlotId> slotId,
        SeatAnchor anchor) {
    public SeatResolution {
        zone = Objects.requireNonNull(zone, "zone");
        slotId = Objects.requireNonNull(slotId, "slotId");
        anchor = Objects.requireNonNull(anchor, "anchor");
    }
}
