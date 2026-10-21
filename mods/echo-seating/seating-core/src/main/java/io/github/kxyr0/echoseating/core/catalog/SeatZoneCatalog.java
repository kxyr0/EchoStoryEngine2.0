package io.github.kxyr0.echoseating.core.catalog;

import io.github.kxyr0.echoseating.api.anchor.SeatAnchor;
import io.github.kxyr0.echoseating.api.diagnostic.SeatingDiagnostic;
import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SeatZoneCatalog {
    private final Map<SeatZoneId, SeatZoneDefinition> zones;
    private final SeatZoneSpatialIndex index;
    private final List<SeatingDiagnostic> diagnostics;
    private final long revision;

    public SeatZoneCatalog(List<SeatZoneDefinition> zones, long revision) {
        Map<SeatZoneId, SeatZoneDefinition> byId = new LinkedHashMap<>();
        zones.forEach(zone -> byId.put(zone.id(), zone));
        this.zones = Map.copyOf(byId);
        this.index = new SeatZoneSpatialIndex(List.copyOf(byId.values()));
        this.diagnostics = SeatZoneDefinitionRules.validateCatalog(List.copyOf(byId.values()));
        this.revision = revision;
    }

    public static SeatZoneCatalog empty() {
        return new SeatZoneCatalog(List.of(), 0L);
    }

    public Optional<SeatZoneDefinition> find(SeatZoneId zoneId) {
        return Optional.ofNullable(zones.get(zoneId));
    }

    public List<SeatZoneDefinition> zones() {
        return List.copyOf(zones.values());
    }

    public List<SeatZoneDefinition> zonesContaining(SeatAnchor anchor) {
        return index.candidates(anchor.dimension(), anchor.blockPosition()).stream()
                .filter(zone -> zone.area().contains(anchor.blockPosition()))
                .toList();
    }

    public List<SeatingDiagnostic> diagnostics() {
        return diagnostics;
    }

    public long revision() {
        return revision;
    }
}
