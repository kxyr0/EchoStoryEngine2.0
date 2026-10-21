package io.github.kxyr0.echoseating.api.zone;

import io.github.kxyr0.echoseating.api.id.SeatSlotId;
import io.github.kxyr0.echoseating.api.world.Vector3;
import java.util.Objects;

public record SeatSlotDefinition(SeatSlotId id, Vector3 anchor) {
    public SeatSlotDefinition {
        id = Objects.requireNonNull(id, "id");
        anchor = Objects.requireNonNull(anchor, "anchor");
    }
}
