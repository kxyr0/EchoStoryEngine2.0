package io.github.kxyr0.echoseating.api.occupancy;

import io.github.kxyr0.echoseating.api.anchor.SeatAnchor;
import io.github.kxyr0.echoseating.api.id.SeatSlotId;
import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.world.PlayerRef;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record SeatOccupancy(
        SeatZoneId zoneId,
        Optional<SeatSlotId> slotId,
        SeatAnchor anchor,
        PlayerRef player,
        UUID vehicleUuid,
        String vehicleEntityTypeId,
        long seatedTick,
        SeatingRevision revision) {
    public SeatOccupancy {
        zoneId = Objects.requireNonNull(zoneId, "zoneId");
        slotId = Objects.requireNonNull(slotId, "slotId");
        anchor = Objects.requireNonNull(anchor, "anchor");
        player = Objects.requireNonNull(player, "player");
        vehicleUuid = Objects.requireNonNull(vehicleUuid, "vehicleUuid");
        vehicleEntityTypeId = Objects.requireNonNull(vehicleEntityTypeId, "vehicleEntityTypeId").trim();
        revision = Objects.requireNonNull(revision, "revision");
        if (vehicleEntityTypeId.isEmpty() || seatedTick < 0L) {
            throw new IllegalArgumentException("SeatOccupancy has invalid vehicle or tick.");
        }
    }
}
