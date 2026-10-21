package io.github.kxyr0.echotime.core.clock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotime.api.value.TimeOfDay;
import org.junit.jupiter.api.Test;

class TimePredicatesTest {
    private final TimePredicates predicates = new TimePredicates();

    @Test
    void betweenWrapsAcrossMidnight() {
        assertTrue(predicates.isBetween(time("23:30"), time("23:00"), time("01:00")));
        assertTrue(predicates.isBetween(time("00:30"), time("23:00"), time("01:00")));
        assertFalse(predicates.isBetween(time("12:00"), time("23:00"), time("01:00")));
    }

    @Test
    void isCoversTheCanonicalMinute() {
        TimeOfDay within = new TimeOfDay(time("09:50").microseconds() + 59_000_000L);
        assertTrue(predicates.is(within, time("09:50")));
        assertFalse(predicates.is(time("09:51"), time("09:50")));
    }

    private TimeOfDay time(String value) {
        return TimeOfDay.parse(value);
    }
}
