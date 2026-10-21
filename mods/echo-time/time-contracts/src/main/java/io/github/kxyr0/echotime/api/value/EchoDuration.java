package io.github.kxyr0.echotime.api.value;

public record EchoDuration(long microseconds) {
    public EchoDuration {
        if (microseconds < 0L) {
            throw new IllegalArgumentException("Echo duration cannot be negative.");
        }
    }

    public static EchoDuration minutes(long minutes) {
        return new EchoDuration(Math.multiplyExact(minutes, TimeOfDay.MICROS_PER_MINUTE));
    }

    public static EchoDuration hours(long hours) {
        return new EchoDuration(Math.multiplyExact(hours, TimeOfDay.MICROS_PER_HOUR));
    }
}
