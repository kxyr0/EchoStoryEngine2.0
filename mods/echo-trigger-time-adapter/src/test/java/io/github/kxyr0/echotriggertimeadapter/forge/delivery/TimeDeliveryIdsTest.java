package io.github.kxyr0.echotriggertimeadapter.forge.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

final class TimeDeliveryIdsTest {
    @Test
    void dailyOccurrencesAreDistinctButFireOnlyOnceIsStable() {
        assertNotEquals(TimeDeliveryIds.delivery("day-1", "sub", "global", false),
                TimeDeliveryIds.delivery("day-2", "sub", "global", false));
        assertEquals(TimeDeliveryIds.delivery("day-1", "sub", "global", true),
                TimeDeliveryIds.delivery("day-2", "sub", "global", true));
    }
}
