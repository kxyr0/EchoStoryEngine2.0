package io.github.kxyr0.echoseating.core.lifecycle;

import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnosticCode;
import io.github.kxyr0.echoseating.api.event.PlayerSeatEvent;
import io.github.kxyr0.echoseating.api.event.PlayerSeatingEventKind;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.occupancy.SeatOccupancy;
import io.github.kxyr0.echoseating.core.catalog.SeatZoneCatalog;
import io.github.kxyr0.echoseating.core.matching.SeatSlotMatcher;
import io.github.kxyr0.echoseating.core.occupancy.SeatOccupancyRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PlayerSeatingTransitionEngine {
    private final SeatSlotMatcher slotMatcher = new SeatSlotMatcher();
    private final SeatOccupancyRegistry occupancy;
    private final SeatEventLedger ledger;
    private final List<SeatingDiagnostic> diagnostics = new ArrayList<>();
    private SeatZoneCatalog catalog;

    public PlayerSeatingTransitionEngine(
            SeatZoneCatalog catalog,
            SeatOccupancyRegistry occupancy,
            SeatEventLedger ledger) {
        this.catalog = catalog;
        this.occupancy = occupancy;
        this.ledger = ledger;
    }

    public void publishCatalog(SeatZoneCatalog catalog) {
        this.catalog = catalog;
        diagnostics.addAll(catalog.diagnostics());
    }

    public List<PlayerSeatEvent> apply(PlayerRidingTransition transition) {
        if (transition.newAnchor().isEmpty()) {
            return stoodUp(transition).stream().toList();
        }
        Optional<SeatResolution> resolution = resolve(transition);
        if (resolution.isEmpty()) {
            return List.of();
        }
        SeatOccupancy next = occupancy(resolution.orElseThrow(), transition);
        Optional<SeatOccupancy> previous = occupancy.active(transition.player());
        if (previous.filter(existing -> sameSeat(existing, next)).isPresent()) {
            return List.of();
        }
        occupancy.save(next).ifPresent(diagnostics::add);
        PlayerSeatingEventKind kind = previous.isPresent()
                ? PlayerSeatingEventKind.CHANGED_SEAT : PlayerSeatingEventKind.SEATED;
        return ledger.publish(kind, transition.player(), next, transition.ridingGeneration(),
                transition.serverTick()).stream().toList();
    }

    public List<SeatingDiagnostic> diagnostics() {
        return List.copyOf(diagnostics);
    }

    private Optional<PlayerSeatEvent> stoodUp(PlayerRidingTransition transition) {
        Optional<SeatOccupancy> previous = occupancy.remove(transition.player());
        return previous.flatMap(value -> ledger.publish(PlayerSeatingEventKind.STOOD_UP,
                transition.player(), value, transition.ridingGeneration(), transition.serverTick()));
    }

    private Optional<SeatResolution> resolve(PlayerRidingTransition transition) {
        var anchor = transition.newAnchor().orElseThrow();
        var zones = catalog.zonesContaining(anchor);
        if (zones.isEmpty()) {
            diagnostics.add(SeatingDiagnostic.error(SeatingDiagnosticCode.SEATING_ANCHOR_OUTSIDE_ZONE,
                    anchor.blockPosition().toString(), "Resolved riding anchor is outside SeatZone catalog."));
            return Optional.empty();
        }
        if (zones.size() > 1) {
            diagnostics.add(SeatingDiagnostic.error(SeatingDiagnosticCode.SEATING_ZONE_OVERLAP_CONFLICT,
                    anchor.blockPosition().toString(), "Anchor matches multiple SeatZones."));
            return Optional.empty();
        }
        var zone = zones.get(0);
        var slot = slotMatcher.match(zone, anchor);
        diagnostics.addAll(slot.diagnostics());
        return slot.publishable() ? Optional.of(new SeatResolution(zone, slot.slotId(), anchor)) : Optional.empty();
    }

    private SeatOccupancy occupancy(SeatResolution resolution, PlayerRidingTransition transition) {
        return new SeatOccupancy(resolution.zone().id(), resolution.slotId(), resolution.anchor(),
                transition.player(), resolution.anchor().vehicleUuid(),
                resolution.anchor().vehicleEntityTypeId(), transition.serverTick(),
                resolution.zone().revision().next());
    }

    private boolean sameSeat(SeatOccupancy left, SeatOccupancy right) {
        return left.zoneId().equals(right.zoneId())
                && left.slotId().equals(right.slotId())
                && left.vehicleUuid().equals(right.vehicleUuid())
                && left.anchor().blockPosition().equals(right.anchor().blockPosition());
    }
}
