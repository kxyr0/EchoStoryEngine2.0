package io.github.kxyr0.echoseating.core.matching;

import io.github.kxyr0.echoseating.api.anchor.SeatAnchor;
import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnosticCode;
import io.github.kxyr0.echoseating.api.zone.SeatDetectionPolicy;
import io.github.kxyr0.echoseating.api.zone.SeatSlotDefinition;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import io.github.kxyr0.echoseating.api.zone.UnknownSeatPolicy;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class SeatSlotMatcher {
    private static final double EPSILON = 0.000001D;

    public SeatSlotMatch match(SeatZoneDefinition zone, SeatAnchor anchor) {
        double radiusSquared = zone.matchRadius() * zone.matchRadius();
        List<SeatSlotDefinition> candidates = zone.slots().stream()
                .filter(slot -> slot.anchor().distanceSquared(anchor.worldPosition()) <= radiusSquared)
                .sorted(Comparator.comparing(slot -> slot.id().value()))
                .toList();
        Optional<SeatSlotDefinition> nearest = nearest(candidates, anchor);
        if (nearest.isPresent() && conflict(candidates, nearest.orElseThrow(), anchor)) {
            return conflict(zone, candidates);
        }
        if (nearest.isPresent()) {
            return new SeatSlotMatch(SeatSlotMatchStatus.NAMED_SLOT,
                    Optional.of(nearest.orElseThrow().id()), List.of());
        }
        return unknown(zone);
    }

    private Optional<SeatSlotDefinition> nearest(List<SeatSlotDefinition> candidates, SeatAnchor anchor) {
        return candidates.stream()
                .min(Comparator.comparingDouble(slot -> slot.anchor().distanceSquared(anchor.worldPosition())));
    }

    private boolean conflict(
            List<SeatSlotDefinition> candidates,
            SeatSlotDefinition nearest,
            SeatAnchor anchor) {
        double distance = nearest.anchor().distanceSquared(anchor.worldPosition());
        return candidates.stream()
                .filter(slot -> !slot.id().equals(nearest.id()))
                .anyMatch(slot -> Math.abs(slot.anchor().distanceSquared(anchor.worldPosition()) - distance)
                        <= EPSILON);
    }

    private SeatSlotMatch unknown(SeatZoneDefinition zone) {
        if (zone.detectionPolicy() == SeatDetectionPolicy.EXACT_SLOT_ONLY
                || zone.unknownSeatPolicy() == UnknownSeatPolicy.REJECT) {
            return new SeatSlotMatch(SeatSlotMatchStatus.UNKNOWN_REJECTED, Optional.empty(),
                    List.of(error(SeatingDiagnosticCode.SEATING_UNKNOWN_SEAT_REJECTED,
                            zone.id().value(), "Unknown seat was rejected by zone policy.")));
        }
        return new SeatSlotMatch(SeatSlotMatchStatus.UNNAMED_ALLOWED, Optional.empty(), List.of());
    }

    private SeatSlotMatch conflict(SeatZoneDefinition zone, List<SeatSlotDefinition> candidates) {
        String subject = candidates.stream().map(slot -> slot.id().value())
                .sorted().reduce((left, right) -> left + "|" + right).orElse(zone.id().value());
        return new SeatSlotMatch(SeatSlotMatchStatus.CONFLICT, Optional.empty(),
                List.of(error(SeatingDiagnosticCode.SEATING_SLOT_MATCH_CONFLICT,
                        subject, "Anchor matches multiple slots at the same distance.")));
    }

    private SeatingDiagnostic error(SeatingDiagnosticCode code, String subject, String message) {
        return SeatingDiagnostic.error(code, subject, message);
    }
}
