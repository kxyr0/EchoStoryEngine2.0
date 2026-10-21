package io.github.kxyr0.echoseating.core.matching;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.id.SeatSlotId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record SeatSlotMatch(
        SeatSlotMatchStatus status,
        Optional<SeatSlotId> slotId,
        List<SeatingDiagnostic> diagnostics) {
    public SeatSlotMatch {
        status = Objects.requireNonNull(status, "status");
        slotId = Objects.requireNonNull(slotId, "slotId");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean publishable() {
        return status == SeatSlotMatchStatus.NAMED_SLOT || status == SeatSlotMatchStatus.UNNAMED_ALLOWED;
    }
}
