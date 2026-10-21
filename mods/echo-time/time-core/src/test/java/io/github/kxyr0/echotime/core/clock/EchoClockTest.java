package io.github.kxyr0.echotime.core.clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.value.TimeRate;
import io.github.kxyr0.echotime.api.value.TimeRevision;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EchoClockTest {
    @Test
    void oneGameMinutePerSimulationSecondHasNoFloatingPointAuthority() {
        EchoClock clock = clock("08:30", "1.0", "1.0");

        for (int tick = 1; tick <= 1_200; tick++) clock.advance(tick, false);

        assertEquals("09:30", clock.instant().timeOfDay().formatHHmm());
        assertEquals(0L, clock.remainder());
    }

    @Test
    void decimalRateAccumulatesExactly() {
        EchoClock clock = clock("00:00", "1.2", "1.0");

        for (int tick = 1; tick <= 20; tick++) clock.advance(tick, false);

        assertEquals(72L * TimeOfDay.MICROS_PER_SECOND, clock.instant().absoluteMicroseconds());
    }

    @Test
    void pauseFreezesOnlyCanonicalProgression() {
        EchoClock clock = clock("08:30", "1.0", "1.0");
        assertFalse(clock.advance(1L, true).advanced());
        assertEquals("08:30", clock.instant().timeOfDay().formatHHmm());
        assertTrue(clock.advance(2L, false).advanced());
    }

    private EchoClock clock(String time, String minutes, String seconds) {
        EchoInstant initial = EchoInstant.at(EchoDay.FIRST, TimeOfDay.parse(time), TimeRevision.INITIAL);
        return new EchoClock(initial, TimeRate.compile(new BigDecimal(minutes), new BigDecimal(seconds)),
                0L, true, 1L);
    }
}
