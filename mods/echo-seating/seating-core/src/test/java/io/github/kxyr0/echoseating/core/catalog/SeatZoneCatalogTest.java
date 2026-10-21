package io.github.kxyr0.echoseating.core.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.world.BlockPosition;
import io.github.kxyr0.echoseating.api.world.DimensionId;
import io.github.kxyr0.echoseating.api.zone.SeatDetectionPolicy;
import io.github.kxyr0.echoseating.api.zone.SeatZoneBox;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import io.github.kxyr0.echoseating.api.zone.UnknownSeatPolicy;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeatZoneCatalogTest {
    @Test
    void reportsOverlappingZones() {
        SeatZoneCatalog catalog = new SeatZoneCatalog(List.of(zone("jdh:a"), zone("jdh:b")), 1L);

        assertFalse(catalog.diagnostics().isEmpty());
        assertEquals(2, catalog.zones().size());
    }

    private SeatZoneDefinition zone(String id) {
        return new SeatZoneDefinition(new SeatZoneId(id), new DimensionId("minecraft:overworld"),
                new SeatZoneBox(new BlockPosition(0, 60, 0), new BlockPosition(2, 62, 2)),
                SeatDetectionPolicy.RIDING_ANCHOR, UnknownSeatPolicy.ALLOW,
                List.of(), 0.85D, SeatingRevision.initial());
    }
}
