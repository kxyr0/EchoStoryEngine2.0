package io.github.kxyr0.echoseating.api.zone;

import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.world.DimensionId;
import java.util.List;
import java.util.Objects;

public record SeatZoneDefinition(
        SeatZoneId id,
        DimensionId dimension,
        SeatZoneBox area,
        SeatDetectionPolicy detectionPolicy,
        UnknownSeatPolicy unknownSeatPolicy,
        List<SeatSlotDefinition> slots,
        double matchRadius,
        SeatingRevision revision) {
    public SeatZoneDefinition {
        id = Objects.requireNonNull(id, "id");
        dimension = Objects.requireNonNull(dimension, "dimension");
        area = Objects.requireNonNull(area, "area");
        detectionPolicy = Objects.requireNonNull(detectionPolicy, "detectionPolicy");
        unknownSeatPolicy = Objects.requireNonNull(unknownSeatPolicy, "unknownSeatPolicy");
        slots = List.copyOf(Objects.requireNonNull(slots, "slots"));
        revision = Objects.requireNonNull(revision, "revision");
        if (matchRadius <= 0.0D) {
            throw new IllegalArgumentException("matchRadius must be positive.");
        }
    }
}
