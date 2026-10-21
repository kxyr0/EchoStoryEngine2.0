package io.github.kxyr0.echotime.forge.daylight;

import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import java.util.Objects;

public final class VanillaDaylightProjection {
    private static final long VANILLA_TICKS_PER_DAY = 24_000L;
    private static final long MIDNIGHT_OFFSET = 18_000L;

    public long derivedDayTime(EchoInstant instant) {
        Objects.requireNonNull(instant, "instant");
        long narrativeDays = instant.day().value() - 1L;
        long dayBase = Math.multiplyExact(narrativeDays, VANILLA_TICKS_PER_DAY);
        long fraction = Math.multiplyExact(instant.timeOfDay().microseconds(), VANILLA_TICKS_PER_DAY)
                / TimeOfDay.MICROS_PER_DAY;
        return Math.addExact(Math.addExact(dayBase, MIDNIGHT_OFFSET), fraction);
    }
}
