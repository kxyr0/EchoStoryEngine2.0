package io.github.kxyr0.echotime.api.value;

import java.util.Objects;

public record EchoInstant(long absoluteMicroseconds, TimeRevision revision) implements Comparable<EchoInstant> {
    public EchoInstant {
        if (absoluteMicroseconds < 0L) {
            throw new IllegalArgumentException("Echo instant cannot precede Day 1 00:00.");
        }
        revision = Objects.requireNonNull(revision, "revision");
    }

    public static EchoInstant at(EchoDay day, TimeOfDay time, TimeRevision revision) {
        long dayBase = Math.multiplyExact(day.value() - 1L, TimeOfDay.MICROS_PER_DAY);
        return new EchoInstant(Math.addExact(dayBase, time.microseconds()), revision);
    }

    public EchoDay day() {
        return new EchoDay(absoluteMicroseconds / TimeOfDay.MICROS_PER_DAY + 1L);
    }

    public TimeOfDay timeOfDay() {
        return new TimeOfDay(absoluteMicroseconds % TimeOfDay.MICROS_PER_DAY);
    }

    public EchoInstant withRevision(TimeRevision nextRevision) {
        return new EchoInstant(absoluteMicroseconds, nextRevision);
    }

    @Override
    public int compareTo(EchoInstant other) {
        return Long.compare(absoluteMicroseconds, other.absoluteMicroseconds);
    }
}
