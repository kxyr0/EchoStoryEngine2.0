package io.github.kxyr0.echoseating.core.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoseating.api.anchor.SeatAnchor;
import io.github.kxyr0.echoseating.api.anchor.SeatAnchorConfidence;
import io.github.kxyr0.echoseating.api.anchor.SeatAnchorSource;
import io.github.kxyr0.echoseating.api.event.PlayerSeatingEventKind;
import io.github.kxyr0.echoseating.api.id.SeatSlotId;
import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.world.BlockPosition;
import io.github.kxyr0.echoseating.api.world.DimensionId;
import io.github.kxyr0.echoseating.api.world.PlayerRef;
import io.github.kxyr0.echoseating.api.world.Vector3;
import io.github.kxyr0.echoseating.api.zone.SeatDetectionPolicy;
import io.github.kxyr0.echoseating.api.zone.SeatSlotDefinition;
import io.github.kxyr0.echoseating.api.zone.SeatZoneBox;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import io.github.kxyr0.echoseating.api.zone.UnknownSeatPolicy;
import io.github.kxyr0.echoseating.core.catalog.SeatZoneCatalog;
import io.github.kxyr0.echoseating.core.occupancy.SeatOccupancyRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerSeatingTransitionEngineTest {
    private final PlayerRef player = new PlayerRef(UUID.fromString("00000000-0000-0000-0000-000000000010"));

    @Test
    void publishesSeatedOnceWhileRidingUnchanged() {
        PlayerSeatingTransitionEngine engine = engine();

        assertEquals(1, engine.apply(mount(anchor(1, 1, 1), 1)).size());
        assertTrue(engine.apply(mount(anchor(1, 1, 1), 2)).isEmpty());
    }

    @Test
    void publishesStoodUpAfterConfirmedDismount() {
        PlayerSeatingTransitionEngine engine = engine();
        engine.apply(mount(anchor(1, 1, 1), 1));

        var events = engine.apply(new PlayerRidingTransition(player, Optional.empty(),
                Optional.empty(), 2L, 2L));

        assertEquals(PlayerSeatingEventKind.STOOD_UP, events.get(0).kind());
    }

    @Test
    void publishesChangedSeatForNewNamedSlot() {
        PlayerSeatingTransitionEngine engine = engine();
        engine.apply(mount(anchor(1, 1, 1), 1));

        var events = engine.apply(mount(anchor(3, 1, 1), 2));

        assertEquals(PlayerSeatingEventKind.CHANGED_SEAT, events.get(0).kind());
        assertEquals(Optional.of(new SeatSlotId("jdh:table/seat_2")), events.get(0).slotId());
    }

    private PlayerSeatingTransitionEngine engine() {
        return new PlayerSeatingTransitionEngine(new SeatZoneCatalog(List.of(zone()), 1L),
                new SeatOccupancyRegistry(), new SeatEventLedger());
    }

    private PlayerRidingTransition mount(SeatAnchor anchor, long generation) {
        return new PlayerRidingTransition(player, Optional.empty(), Optional.of(anchor), generation, generation);
    }

    private SeatZoneDefinition zone() {
        return new SeatZoneDefinition(new SeatZoneId("jdh:table"), new DimensionId("minecraft:overworld"),
                new SeatZoneBox(new BlockPosition(0, 0, 0), new BlockPosition(5, 5, 5)),
                SeatDetectionPolicy.RIDING_ANCHOR, UnknownSeatPolicy.ALLOW, List.of(
                new SeatSlotDefinition(new SeatSlotId("jdh:table/seat_1"), new Vector3(1, 1, 1)),
                new SeatSlotDefinition(new SeatSlotId("jdh:table/seat_2"), new Vector3(3, 1, 1))),
                0.85D, SeatingRevision.initial());
    }

    private SeatAnchor anchor(double x, double y, double z) {
        return new SeatAnchor(new DimensionId("minecraft:overworld"), new Vector3(x, y, z),
                BlockPosition.containing(new Vector3(x, y, z)), SeatAnchorSource.VEHICLE_BLOCK_POSITION,
                SeatAnchorConfidence.MEDIUM, UUID.fromString("00000000-0000-0000-0000-000000000020"),
                "test:seat", 1L, SeatingRevision.initial());
    }
}
