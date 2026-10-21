package io.github.kxyr0.echotime.forge.daylight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.value.TimeRevision;
import org.junit.jupiter.api.Test;

class VanillaDaylightProjectionTest {
    private final VanillaDaylightProjection projection = new VanillaDaylightProjection();

    @Test
    void canonicalMinecraftPhasesRemainMonotonic() {
        assertEquals(18_000L, time(1, "00:00"));
        assertEquals(24_000L, time(1, "06:00"));
        assertEquals(30_000L, time(1, "12:00"));
        assertEquals(36_000L, time(1, "18:00"));
        assertEquals(42_000L, time(2, "00:00"));
    }

    private long time(long day, String value) {
        return projection.derivedDayTime(EchoInstant.at(new EchoDay(day), TimeOfDay.parse(value),
                TimeRevision.INITIAL));
    }
}
