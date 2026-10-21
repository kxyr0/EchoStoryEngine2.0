package io.github.kxyr0.echotriggerseatingadapter.forge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoseating.api.anchor.SeatAnchor;
import io.github.kxyr0.echoseating.api.anchor.SeatAnchorConfidence;
import io.github.kxyr0.echoseating.api.anchor.SeatAnchorSource;
import io.github.kxyr0.echoseating.api.event.PlayerSeatEvent;
import io.github.kxyr0.echoseating.api.event.PlayerSeatingEventKind;
import io.github.kxyr0.echoseating.api.id.SeatEventId;
import io.github.kxyr0.echoseating.api.id.SeatSlotId;
import io.github.kxyr0.echoseating.api.id.SeatZoneId;
import io.github.kxyr0.echoseating.api.id.SeatingRevision;
import io.github.kxyr0.echoseating.api.world.BlockPosition;
import io.github.kxyr0.echoseating.api.world.DimensionId;
import io.github.kxyr0.echoseating.api.world.PlayerRef;
import io.github.kxyr0.echoseating.api.world.Vector3;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SeatingSubscriptionIndexTest {
    @Test
    void namedSeatMatchesGenericAndSpecificSubscriptions() {
        TriggerStorySubscription generic = subscription("generic", Optional.empty());
        TriggerStorySubscription specific = subscription("specific", Optional.of("jdh:table/seat_3"));

        List<TriggerStorySubscription> matches = SeatingSubscriptionIndex.from(List.of(generic, specific))
                .matches(event(Optional.of(new SeatSlotId("jdh:table/seat_3"))));

        assertEquals(List.of(generic, specific), matches);
    }

    @Test
    void namedSeatMatchesSlotOnlySubscription() {
        TriggerStorySubscription slotOnly = slotOnlySubscription("slot_only", "jdh:table/seat_3");

        List<TriggerStorySubscription> matches = SeatingSubscriptionIndex.from(List.of(slotOnly))
                .matches(event(Optional.of(new SeatSlotId("jdh:table/seat_3"))));

        assertEquals(List.of(slotOnly), matches);
    }

    @Test
    void unnamedSeatMatchesOnlyGenericSubscription() {
        TriggerStorySubscription generic = subscription("generic", Optional.empty());
        TriggerStorySubscription specific = subscription("specific", Optional.of("jdh:table/seat_3"));

        List<TriggerStorySubscription> matches = SeatingSubscriptionIndex.from(List.of(generic, specific))
                .matches(event(Optional.empty()));

        assertEquals(List.of(generic), matches);
    }

    private static TriggerStorySubscription subscription(String id, Optional<String> slotId) {
        return new TriggerStorySubscription(new TriggerSubscriptionId("jdh:" + id),
                new TriggerId("jdh:" + id),
                TriggerEventSelector.seating("echo_seating:events", "SEATED", "jdh:table", slotId),
                "jdh:chapter_1/" + id, true);
    }

    private static TriggerStorySubscription slotOnlySubscription(String id, String slotId) {
        return new TriggerStorySubscription(new TriggerSubscriptionId("jdh:" + id),
                new TriggerId("jdh:" + id),
                TriggerEventSelector.seatingAt("echo_seating:events", "SEATED", slotId),
                "jdh:chapter_1/" + id, true);
    }

    private static PlayerSeatEvent event(Optional<SeatSlotId> slotId) {
        return new PlayerSeatEvent(new SeatEventId("seat-event:test"), PlayerSeatingEventKind.SEATED,
                new PlayerRef(UUID.fromString("00000000-0000-0000-0000-000000000050")),
                new SeatZoneId("jdh:table"), slotId, anchor(), "test:seat", 10L,
                SeatingRevision.initial(), "fingerprint");
    }

    private static SeatAnchor anchor() {
        return new SeatAnchor(new DimensionId("minecraft:overworld"), new Vector3(1, 1, 1),
                new BlockPosition(1, 1, 1), SeatAnchorSource.VEHICLE_BLOCK_POSITION,
                SeatAnchorConfidence.MEDIUM, UUID.fromString("00000000-0000-0000-0000-000000000051"),
                "test:seat", 10L, SeatingRevision.initial());
    }
}
