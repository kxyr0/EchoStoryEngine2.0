package io.github.kxyr0.echotime.api.snapshot;

import io.github.kxyr0.echotime.api.value.EchoInstant;
import io.github.kxyr0.echotime.api.value.TimeRate;
import java.util.Objects;

public record EchoTimeSnapshot(
        EchoInstant instant,
        boolean configuredRunning,
        boolean paused,
        TimeRate effectiveRate,
        long serverTick,
        long configRevision) {
    public EchoTimeSnapshot {
        instant = Objects.requireNonNull(instant, "instant");
        effectiveRate = Objects.requireNonNull(effectiveRate, "effectiveRate");
        if (serverTick < 0L || configRevision < 0L) {
            throw new IllegalArgumentException("Snapshot revisions and ticks cannot be negative.");
        }
    }

    public boolean running() {
        return configuredRunning && !paused && !effectiveRate.stopped();
    }
}
