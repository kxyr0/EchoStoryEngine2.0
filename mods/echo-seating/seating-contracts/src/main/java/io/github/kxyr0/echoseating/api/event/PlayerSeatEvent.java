package io.github.kxyr0.echoseating.api.event;

import io.github.kxyr0.echoseating.api.anchor.SeatAnchor;
import io.github.kxyr0.echoseating.api.id.SeatEventId;
import io.github.kxyr0.echoseating.api.id.SeatSlotId;
import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.world.PlayerRef;
import java.util.Objects;
import java.util.Optional;

public record PlayerSeatEvent(
        SeatEventId eventId,
        PlayerSeatingEventKind kind,
        PlayerRef player,
        SeatZoneId zoneId,
        Optional<SeatSlotId> slotId,
        SeatAnchor anchor,
        String vehicleEntityTypeId,
        long serverTick,
        SeatingRevision occupancyRevision,
        String fingerprint) {
    public PlayerSeatEvent {
        eventId = Objects.requireNonNull(eventId, "eventId");
        kind = Objects.requireNonNull(kind, "kind");
        player = Objects.requireNonNull(player, "player");
        zoneId = Objects.requireNonNull(zoneId, "zoneId");
        slotId = Objects.requireNonNull(slotId, "slotId");
        anchor = Objects.requireNonNull(anchor, "anchor");
        vehicleEntityTypeId = Objects.requireNonNull(vehicleEntityTypeId, "vehicleEntityTypeId").trim();
        occupancyRevision = Objects.requireNonNull(occupancyRevision, "occupancyRevision");
        fingerprint = Objects.requireNonNull(fingerprint, "fingerprint").trim();
        if (vehicleEntityTypeId.isEmpty() || fingerprint.isEmpty() || serverTick < 0L) {
            throw new IllegalArgumentException("PlayerSeatEvent has invalid event payload.");
        }
    }
}
