package io.github.kxyr0.echodebug.forge.integration.seating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.kxyr0.echodebug.core.selection.TriggerBlockPoint;

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
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import io.github.kxyr0.echotrigger.api.subscription.TriggerStorySubscription;
import io.github.kxyr0.echotrigger.api.subscription.TriggerSubscriptionId;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeatingTriggerAreaProviderTest {
    @Test
    void wandResolvesClickedChairAndAnchorToTheSameExactSlot() {
        var chair = SeatSelectionResolver.resolve(List.of(zone()),
                new TriggerBlockPoint("minecraft:overworld", 38, -4, 97));
        var anchor = SeatSelectionResolver.resolve(List.of(zone()),
                new TriggerBlockPoint("minecraft:overworld", 38, -3, 97));
        assertEquals("jdh:home/breakfast_table", chair.zoneId());
        assertEquals("jdh:home/breakfast_table/john", chair.slotId());
        assertEquals(chair, anchor);
    }

    @Test
    void wandRejectsUnregisteredBlocksAndOtherDimensions() {
        assertThrows(IllegalArgumentException.class, () -> SeatSelectionResolver.resolve(List.of(zone()),
                new TriggerBlockPoint("minecraft:overworld", 39, -4, 97)));
        assertThrows(IllegalArgumentException.class, () -> SeatSelectionResolver.resolve(List.of(zone()),
                new TriggerBlockPoint("minecraft:the_nether", 38, -4, 97)));
    }

    @Test
    void wandRejectsAmbiguousSeatsInsteadOfPickingAnArbitraryOne() {
        SeatZoneDefinition original = zone();
        SeatZoneDefinition overlap = new SeatZoneDefinition(original.id(), original.dimension(), original.area(),
                original.detectionPolicy(), original.unknownSeatPolicy(),
                List.of(slot("overlap", 38, 97)), original.matchRadius(), original.revision());
        assertThrows(IllegalArgumentException.class, () -> SeatSelectionResolver.resolve(List.of(original, overlap),
                new TriggerBlockPoint("minecraft:overworld", 38, -4, 97)));
    }

    @Test
    void resolvesOnlySlotsReferencedBySpatialTriggerSubscriptions() {
        var areas = SeatingTriggerAreaProvider.areas(List.of(
                subscription("john"), subscription("father"), subscription("mother"), sleepSubscription()),
                List.of(zone()), new TriggerRevision(3));

        assertEquals(3, areas.size());
        assertEquals(List.of("jdh:home/breakfast_table/father", "jdh:home/breakfast_table/john",
                        "jdh:home/breakfast_table/mother"),
                areas.stream().map(area -> area.areaName()).sorted().toList());
        var john = areas.stream().filter(area -> area.areaName().endsWith("/john")).findFirst().orElseThrow();
        assertEquals(38.0D, john.geometry().min().X());
        assertEquals(-4.0D, john.geometry().min().Y());
        assertEquals(98.0D, john.geometry().max().Z());
    }

    private static TriggerStorySubscription subscription(String seat) {
        String triggerId = "jdh:trigger/sat_on_" + seat + "_seat";
        String slotId = "jdh:home/breakfast_table/" + seat;
        return new TriggerStorySubscription(new TriggerSubscriptionId(triggerId + "#story"),
                new TriggerId(triggerId), TriggerEventSelector.seatingAt("echo_seating:events", "SEATED", slotId),
                "jdh:test_story", false);
    }

    private static TriggerStorySubscription sleepSubscription() {
        return new TriggerStorySubscription(new TriggerSubscriptionId("jdh:first_night_sleep#story"),
                new TriggerId("jdh:first_night_sleep"),
                TriggerEventSelector.of("echo_player_sleep:events", "CONFIRMED", "jdh:first_night"),
                "jdh:first_night_dream", false);
    }

    private static SeatZoneDefinition zone() {
        return new SeatZoneDefinition(new SeatZoneId("jdh:home/breakfast_table"),
                new DimensionId("minecraft:overworld"),
                new SeatZoneBox(new BlockPosition(35, -3, 96), new BlockPosition(40, -2, 104)),
                SeatDetectionPolicy.RIDING_ANCHOR, UnknownSeatPolicy.REJECT,
                List.of(slot("john", 38, 97), slot("sasha", 37, 97),
                        slot("father", 38, 100), slot("mother", 37, 100)),
                0.85D, new SeatingRevision(1));
    }

    private static SeatSlotDefinition slot(String name, double x, double z) {
        return new SeatSlotDefinition(new SeatSlotId("jdh:home/breakfast_table/" + name),
                new Vector3(x, -3.0D, z));
    }
}
