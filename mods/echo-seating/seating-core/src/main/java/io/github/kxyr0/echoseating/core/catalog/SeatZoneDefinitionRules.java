package io.github.kxyr0.echoseating.core.catalog;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnosticCode;
import io.github.kxyr0.echoseating.api.id.SeatSlotId;
import io.github.kxyr0.echoseating.api.world.BlockPosition;
import io.github.kxyr0.echoseating.api.zone.SeatSlotDefinition;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SeatZoneDefinitionRules {
    private SeatZoneDefinitionRules() {
    }

    public static List<SeatingDiagnostic> validate(SeatZoneDefinition zone) {
        List<SeatingDiagnostic> diagnostics = new ArrayList<>();
        if (zone.matchRadius() <= 0.0D) {
            diagnostics.add(error(SeatingDiagnosticCode.SEATING_SLOT_INVALID,
                    zone.id().value(), "MatchRadius must be positive."));
        }
        validateSlots(zone, diagnostics);
        return List.copyOf(diagnostics);
    }

    public static List<SeatingDiagnostic> validateCatalog(List<SeatZoneDefinition> zones) {
        List<SeatingDiagnostic> diagnostics = new ArrayList<>();
        for (int left = 0; left < zones.size(); left++) {
            for (int right = left + 1; right < zones.size(); right++) {
                validatePair(zones.get(left), zones.get(right), diagnostics);
            }
        }
        return List.copyOf(diagnostics);
    }

    private static void validateSlots(
            SeatZoneDefinition zone,
            List<SeatingDiagnostic> diagnostics) {
        Set<SeatSlotId> seen = new LinkedHashSet<>();
        for (SeatSlotDefinition slot : zone.slots()) {
            if (!seen.add(slot.id())) {
                diagnostics.add(error(SeatingDiagnosticCode.SEATING_SLOT_DUPLICATE,
                        slot.id().value(), "SeatSlotId is duplicated in this zone."));
            }
            BlockPosition block = BlockPosition.containing(slot.anchor());
            if (!zone.area().contains(block)) {
                diagnostics.add(error(SeatingDiagnosticCode.SEATING_SLOT_OUTSIDE_ZONE,
                        slot.id().value(), "SeatSlot anchor is outside SeatZone area."));
            }
        }
    }

    private static void validatePair(
            SeatZoneDefinition left,
            SeatZoneDefinition right,
            List<SeatingDiagnostic> diagnostics) {
        if (!left.dimension().equals(right.dimension()) || !left.area().overlaps(right.area())) {
            return;
        }
        diagnostics.add(error(SeatingDiagnosticCode.SEATING_ZONE_OVERLAP_CONFLICT,
                left.id().value() + "|" + right.id().value(), "SeatZone boxes overlap in one dimension."));
        left.slots().stream()
                .filter(slot -> right.slots().stream().anyMatch(other -> other.id().equals(slot.id())))
                .findFirst()
                .ifPresent(slot -> diagnostics.add(error(SeatingDiagnosticCode.SEATING_SLOT_MATCH_CONFLICT,
                        slot.id().value(), "SeatSlotId is shared by overlapping zones.")));
    }

    private static SeatingDiagnostic error(SeatingDiagnosticCode code, String subject, String message) {
        return SeatingDiagnostic.error(code, subject, message);
    }
}
