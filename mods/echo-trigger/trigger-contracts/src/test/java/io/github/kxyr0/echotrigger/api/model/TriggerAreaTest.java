package io.github.kxyr0.echotrigger.api.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerAreaTest {
    @Test
    void oneBlockAreaUsesMinInclusiveAndMaxExclusiveBounds() {
        TriggerArea area = TriggerArea.block("minecraft:overworld", 200, 4, 200);

        assertTrue(area.contains("minecraft:overworld", 200.0, 4.0, 200.0));
        assertTrue(area.contains("minecraft:overworld", 200.99, 4.99, 200.99));
        assertFalse(area.contains("minecraft:overworld", 201.0, 4.0, 200.0));
        assertFalse(area.contains("minecraft:the_nether", 200.0, 4.0, 200.0));
    }

    @Test
    void sizedAreaAcceptsDecimalPlayerCoordinatesInsideVolume() {
        TriggerArea area = new TriggerArea("minecraft:overworld", 10, 20, 30, 3, 4, 5);

        assertTrue(area.contains("minecraft:overworld", 12.75, 23.99, 34.99));
        assertFalse(area.contains("minecraft:overworld", 13.0, 23.99, 34.99));
    }

    @Test
    void fromToCreatesInclusiveBlockRangeAsExclusiveRuntimeBounds() {
        TriggerArea area = TriggerArea.fromTo("minecraft:overworld",
                new TriggerBlockPos(3, 4, 5), new TriggerBlockPos(1, 2, 3));

        assertTrue(area.contains("minecraft:overworld", 1, 2, 3));
        assertTrue(area.contains("minecraft:overworld", 3.99, 4.99, 5.99));
        assertFalse(area.contains("minecraft:overworld", 4, 5, 6));
    }

    @Test
    void invalidDimensionAndSizeFailFast() {
        assertThrows(IllegalArgumentException.class, () -> TriggerArea.block("overworld", 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new TriggerArea("minecraft:overworld", 0, 0, 0, 0, 1, 1));
    }
}
