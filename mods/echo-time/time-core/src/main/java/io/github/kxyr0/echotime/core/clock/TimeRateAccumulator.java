package io.github.kxyr0.echotime.core.clock;

import io.github.kxyr0.echotime.api.value.TimeRate;
import java.math.BigInteger;
import java.util.Objects;

public final class TimeRateAccumulator {
    private TimeRate rate;
    private long remainder;

    public TimeRateAccumulator(TimeRate rate, long remainder) {
        this.rate = Objects.requireNonNull(rate, "rate");
        if (remainder < 0L || remainder >= rate.simulationTicks()) {
            throw new IllegalArgumentException("Rate remainder must be within its denominator.");
        }
        this.remainder = remainder;
    }

    public long advance() {
        long total = Math.addExact(remainder, rate.gameMicroseconds());
        long whole = total / rate.simulationTicks();
        remainder = total % rate.simulationTicks();
        return whole;
    }

    public void replaceRate(TimeRate next) {
        Objects.requireNonNull(next, "next");
        BigInteger scaled = BigInteger.valueOf(remainder)
                .multiply(BigInteger.valueOf(next.simulationTicks()))
                .divide(BigInteger.valueOf(rate.simulationTicks()));
        rate = next;
        remainder = scaled.longValueExact();
    }

    public TimeRate rate() {
        return rate;
    }

    public long remainder() {
        return remainder;
    }
}
