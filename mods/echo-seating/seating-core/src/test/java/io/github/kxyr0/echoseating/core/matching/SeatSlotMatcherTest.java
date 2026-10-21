package io.github.kxyr0.echoseating.core.matching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoseating.api.anchor.SeatAnchor;
import io.github.kxyr0.echoseating.api.anchor.SeatAnchorConfidence;
import io.github.kxyr0.echoseating.api.anchor.SeatAnchorSource;
import io.github.kxyr0.echoseating.api.id.SeatSlotId;
import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.world.BlockPosition;
import io.github.kxyr0.echoseating.api.world.DimensionId;
import io.github.kxyr0.echoseating.api.world.Vector3;
import io.github.kxyr0.echoseating.api.zone.SeatDetectionPolicy;
import io.github.kxyr0.echoseating.api.zone.SeatSlotDefinition;
import io.github.kxyr0.echoseating.api.zone.SeatZoneBox;
import io.github.kxyr0.echoseating.api.zone.SeatZoneDefinition;
import io.github.kxyr0.echoseating.api.zone.UnknownSeatPolicy;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SeatSlotMatcherTest {
    private final SeatSlotMatcher matcher = new SeatSlotMatcher();

    @Test
    void matchesNamedSlotWithinRadius() {
        SeatSlotMatch match = matcher.match(zone(UnknownSeatPolicy.ALLOW), anchor(1, 1, 1));

        assertEquals(SeatSlotMatchStatus.NAMED_SLOT, match.status());
        assertEquals(Optional.of(new SeatSlotId("jdh:table/seat_1")), match.slotId());
    }

    @Test
    void allowsUnknownSeatWhenPolicyAllows() {
        SeatSlotMatch match = matcher.match(zone(UnknownSeatPolicy.ALLOW), anchor(4, 1, 4));

        assertEquals(SeatSlotMatchStatus.UNNAMED_ALLOWED, match.status());
        assertTrue(match.slotId().isEmpty());
    }

    @Test
    void rejectsUnknownSeatWhenPolicyRejects() {
        SeatSlotMatch match = matcher.match(zone(UnknownSeatPolicy.REJECT), anchor(4, 1, 4));

        assertEquals(SeatSlotMatchStatus.UNKNOWN_REJECTED, match.status());
    }

    @Test
    void reportsEqualDistanceConflict() {
        SeatZoneDefinition zone = new SeatZoneDefinition(new SeatZoneId("jdh:table"),
                new DimensionId("minecraft:overworld"),
                new SeatZoneBox(new BlockPosition(0, 0, 0), new BlockPosition(3, 3, 3)),
                SeatDetectionPolicy.RIDING_ANCHOR, UnknownSeatPolicy.ALLOW, List.of(
                new SeatSlotDefinition(new SeatSlotId("jdh:table/a"), new Vector3(1, 1, 1)),
                new SeatSlotDefinition(new SeatSlotId("jdh:table/b"), new Vector3(1, 1, 3))),
                2.0D, SeatingRevision.initial());

        SeatSlotMatch match = matcher.match(zone, anchor(1, 1, 2));

        assertEquals(SeatSlotMatchStatus.CONFLICT, match.status());
    }

    private SeatZoneDefinition zone(UnknownSeatPolicy policy) {
        return new SeatZoneDefinition(new SeatZoneId("jdh:table"), new DimensionId("minecraft:overworld"),
                new SeatZoneBox(new BlockPosition(0, 0, 0), new BlockPosition(5, 5, 5)),
                SeatDetectionPolicy.RIDING_ANCHOR, policy, List.of(
                new SeatSlotDefinition(new SeatSlotId("jdh:table/seat_1"), new Vector3(1, 1, 1))),
                0.85D, SeatingRevision.initial());
    }

    private SeatAnchor anchor(double x, double y, double z) {
        return new SeatAnchor(new DimensionId("minecraft:overworld"), new Vector3(x, y, z),
                BlockPosition.containing(new Vector3(x, y, z)), SeatAnchorSource.VEHICLE_BLOCK_POSITION,
                SeatAnchorConfidence.MEDIUM, UUID.fromString("00000000-0000-0000-0000-000000000001"),
                "test:seat", 1L, SeatingRevision.initial());
    }
}
