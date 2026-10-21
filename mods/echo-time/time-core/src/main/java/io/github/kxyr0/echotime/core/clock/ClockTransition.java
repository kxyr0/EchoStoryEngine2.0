package io.github.kxyr0.echotime.core.clock;

import io.github.kxyr0.echotime.api.value.EchoInstant;
import java.util.Objects;

public record ClockTransition(EchoInstant previous, EchoInstant current, long serverTick) {
    public ClockTransition {
        previous = Objects.requireNonNull(previous, "previous");
        current = Objects.requireNonNull(current, "current");
        if (serverTick < 0L) throw new IllegalArgumentException("serverTick cannot be negative.");
    }

    public boolean advanced() {
        return current.absoluteMicroseconds() > previous.absoluteMicroseconds();
    }

    public long elapsedMicroseconds() {
        return current.absoluteMicroseconds() - previous.absoluteMicroseconds();
    }
}
