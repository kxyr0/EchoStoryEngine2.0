package io.github.kxyr0.echotime.core.clock;

import io.github.kxyr0.echotime.api.snapshot.EchoTimeSnapshot;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeRate;
import io.github.kxyr0.echotime.api.value.TimeRevision;
import java.util.Objects;

public final class EchoClock {
    private EchoInstant instant;
    private final TimeRateAccumulator accumulator;
    private boolean configuredRunning;
    private long configRevision;

    public EchoClock(
            EchoInstant initial,
            TimeRate rate,
            long remainder,
            boolean configuredRunning,
            long configRevision) {
        instant = Objects.requireNonNull(initial, "initial");
        accumulator = new TimeRateAccumulator(rate, remainder);
        if (configRevision < 0L) throw new IllegalArgumentException("configRevision cannot be negative.");
        this.configuredRunning = configuredRunning;
        this.configRevision = configRevision;
    }

    public ClockTransition advance(long serverTick, boolean paused) {
        EchoInstant previous = instant;
        if (!configuredRunning || paused || accumulator.rate().stopped()) {
            return new ClockTransition(previous, instant, serverTick);
        }
        long elapsed = accumulator.advance();
        if (elapsed > 0L) {
            instant = new EchoInstant(Math.addExact(instant.absoluteMicroseconds(), elapsed), nextRevision());
        }
        return new ClockTransition(previous, instant, serverTick);
    }

    public ClockTransition setAbsolute(long absoluteMicroseconds, long serverTick) {
        EchoInstant previous = instant;
        instant = new EchoInstant(absoluteMicroseconds, nextRevision());
        return new ClockTransition(previous, instant, serverTick);
    }

    public void applyConfig(TimeRate rate, boolean running, long revision) {
        accumulator.replaceRate(Objects.requireNonNull(rate, "rate"));
        configuredRunning = running;
        configRevision = revision;
        instant = instant.withRevision(nextRevision());
    }

    public EchoTimeSnapshot snapshot(long serverTick, boolean paused) {
        return new EchoTimeSnapshot(instant, configuredRunning, paused, accumulator.rate(), serverTick, configRevision);
    }

    public EchoInstant instant() {
        return instant;
    }

    public TimeRate rate() {
        return accumulator.rate();
    }

    public long remainder() {
        return accumulator.remainder();
    }

    public boolean configuredRunning() {
        return configuredRunning;
    }

    public long configRevision() {
        return configRevision;
    }

    private TimeRevision nextRevision() {
        return instant.revision().next();
    }
}
