package io.github.kxyr0.echotime.core.clock;

import io.github.kxyr0.echotime.api.value.TimeOfDay;
import java.util.Objects;

public final class TimePredicates {
    public boolean is(TimeOfDay current, TimeOfDay expected) {
        return minute(current) == minute(expected);
    }

    public boolean isBefore(TimeOfDay current, TimeOfDay expected) {
        return current.compareTo(expected) < 0;
    }

    public boolean isAfter(TimeOfDay current, TimeOfDay expected) {
        return current.compareTo(expected) > 0;
    }

    public boolean isBetween(TimeOfDay current, TimeOfDay start, TimeOfDay end) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (start.compareTo(end) <= 0) {
            return current.compareTo(start) >= 0 && current.compareTo(end) <= 0;
        }
        return current.compareTo(start) >= 0 || current.compareTo(end) <= 0;
    }

    private long minute(TimeOfDay value) {
        return value.microseconds() / TimeOfDay.MICROS_PER_MINUTE;
    }
}
