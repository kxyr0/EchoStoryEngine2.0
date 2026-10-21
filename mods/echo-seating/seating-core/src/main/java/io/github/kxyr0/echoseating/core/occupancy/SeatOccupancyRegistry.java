package io.github.kxyr0.echoseating.core.occupancy;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnosticCode;
import io.github.kxyr0.echoseating.api.id.SeatSlotId;
import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.occupancy.SeatOccupancy;
import io.github.kxyr0.echoseating.api.world.PlayerRef;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SeatOccupancyRegistry {
    private final Map<PlayerRef, SeatOccupancy> byPlayer = new LinkedHashMap<>();

    public Optional<SeatOccupancy> active(PlayerRef player) {
        return Optional.ofNullable(byPlayer.get(player));
    }

    public List<SeatOccupancy> activeOccupancies() {
        return List.copyOf(byPlayer.values());
    }

    public Optional<SeatingDiagnostic> save(SeatOccupancy occupancy) {
        Optional<SeatingDiagnostic> conflict = exactSlotConflict(occupancy);
        byPlayer.put(occupancy.player(), occupancy);
        return conflict;
    }

    public Optional<SeatOccupancy> remove(PlayerRef player) {
        return Optional.ofNullable(byPlayer.remove(player));
    }

    private Optional<SeatingDiagnostic> exactSlotConflict(SeatOccupancy occupancy) {
        if (occupancy.slotId().isEmpty()) {
            return Optional.empty();
        }
        SeatZoneId zoneId = occupancy.zoneId();
        SeatSlotId slotId = occupancy.slotId().orElseThrow();
        return byPlayer.values().stream()
                .filter(existing -> !existing.player().equals(occupancy.player()))
                .filter(existing -> existing.zoneId().equals(zoneId))
                .filter(existing -> existing.slotId().filter(slotId::equals).isPresent())
                .findFirst()
                .map(existing -> SeatingDiagnostic.error(
                        SeatingDiagnosticCode.SEATING_SLOT_OCCUPANCY_CONFLICT,
                        slotId.value(), "Two players occupy one exact SeatSlotId."));
    }
}
