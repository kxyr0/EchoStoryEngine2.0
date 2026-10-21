package io.github.kxyr0.echotime.api.value;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record TimeOfDay(long microseconds) implements Comparable<TimeOfDay> {
    public static final long MICROS_PER_SECOND = 1_000_000L;
    public static final long MICROS_PER_MINUTE = 60L * MICROS_PER_SECOND;
    public static final long MICROS_PER_HOUR = 60L * MICROS_PER_MINUTE;
    public static final long MICROS_PER_DAY = 24L * MICROS_PER_HOUR;
    private static final Pattern STRICT = Pattern.compile("([01]\\d|2[0-3]):([0-5]\\d)");

    public TimeOfDay {
        if (microseconds < 0L || microseconds >= MICROS_PER_DAY) {
            throw new IllegalArgumentException("Time of day must be within 00:00:00.000000..23:59:59.999999.");
        }
    }

    public static TimeOfDay parse(String value) {
        Matcher matcher = STRICT.matcher(value == null ? "" : value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Time must use strict 24-hour HH:mm format.");
        }
        long hour = Long.parseLong(matcher.group(1));
        long minute = Long.parseLong(matcher.group(2));
        return new TimeOfDay(hour * MICROS_PER_HOUR + minute * MICROS_PER_MINUTE);
    }

    public int hour() {
        return (int) (microseconds / MICROS_PER_HOUR);
    }

    public int minute() {
        return (int) ((microseconds / MICROS_PER_MINUTE) % 60L);
    }

    public int second() {
        return (int) ((microseconds / MICROS_PER_SECOND) % 60L);
    }

    public String formatHHmm() {
        return String.format(Locale.ROOT, "%02d:%02d", hour(), minute());
    }

    @Override
    public int compareTo(TimeOfDay other) {
        return Long.compare(microseconds, other.microseconds);
    }
}
