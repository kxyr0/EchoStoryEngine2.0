package io.github.kxyr0.echotriggertimeadapter.forge.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotrigger.api.subscription.TriggerEventSelector;
import org.junit.jupiter.api.Test;

final class TimeSelectorParserTest {
    private final TimeSelectorParser parser = new TimeSelectorParser();

    @Test
    void parsesDailyAndSpecificDaySelectors() {
        var daily = parser.parse(TriggerEventSelector.of(
                "echo_time:occurrences", "AT", "echo_time:daily/09_50")).orElseThrow();
        var specific = parser.parse(TriggerEventSelector.of(
                "echo_time:occurrences", "AT", "echo_time:day/2/10_00")).orElseThrow();

        assertTrue(daily.day().isEmpty());
        assertEquals("09:50", daily.time().formatHHmm());
        assertEquals(2L, specific.day().orElseThrow().value());
        assertEquals("10:00", specific.time().formatHHmm());
    }
}
