package io.github.kxyr0.echoseating.forge.api;

import io.github.kxyr0.echoseating.forge.api.view.SeatOccupancyView;
import io.github.kxyr0.echoseating.forge.api.view.SeatZoneCatalogView;
import io.github.kxyr0.echoseating.forge.api.view.SeatingDiagnosticsView;
import io.github.kxyr0.echoseating.forge.api.view.SeatingEventView;
import java.util.Objects;

public record SeatingServiceBridge(
        SeatZoneCatalogView catalog,
        SeatOccupancyView occupancy,
        SeatingEventView events,
        SeatingDiagnosticsView diagnostics) {
    public SeatingServiceBridge {
        catalog = Objects.requireNonNull(catalog, "catalog");
        occupancy = Objects.requireNonNull(occupancy, "occupancy");
        events = Objects.requireNonNull(events, "events");
        diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }
}
