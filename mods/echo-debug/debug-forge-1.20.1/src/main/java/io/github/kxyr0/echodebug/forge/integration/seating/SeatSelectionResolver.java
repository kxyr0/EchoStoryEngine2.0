package io.github.kxyr0.echodebug.forge.integration.seating;

import io.github.kxyr0.echodebug.core.selection.TriggerBlockPoint;
import io.github.kxyr0.echoseating.api.world.BlockPosition;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import java.util.List;

public final class SeatSelectionResolver {
    private SeatSelectionResolver() {
    }

    public static SeatReference resolve(List<SeatZoneDefinition> zones, TriggerBlockPoint point) {
        List<SeatReference> matches = zones.stream()
                .filter(zone -> zone.dimension().value().equals(point.dimension()))
                .flatMap(zone -> zone.slots().stream()
                        .filter(slot -> matches(BlockPosition.containing(slot.anchor()), point))
                        .map(slot -> new SeatReference(zone.id().value(), slot.id().value())))
                .distinct().toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Здесь нет зарегистрированного сиденья Echo Seating.");
        }
        if (matches.size() != 1) {
            throw new IllegalArgumentException("Здесь несколько сидений: уточните их координаты в Echo Seating.");
        }
        return matches.get(0);
    }

    private static boolean matches(BlockPosition anchor, TriggerBlockPoint point) {
        return anchor.x() == point.x() && anchor.z() == point.z()
                && (anchor.y() == point.y() || anchor.y() - 1 == point.y());
    }

    public record SeatReference(String zoneId, String slotId) {
    }
}
