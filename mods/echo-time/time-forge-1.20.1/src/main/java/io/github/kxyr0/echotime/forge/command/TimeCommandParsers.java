package io.github.kxyr0.echotime.forge.command;

import io.github.kxyr0.echotime.api.value.EchoDuration;

final class TimeCommandParsers {
    private TimeCommandParsers() { }

    static EchoDuration duration(String raw) {
        if (raw == null || raw.length() < 2) throw new IllegalArgumentException("Duration must look like 10m or 2h.");
        long value = Long.parseLong(raw.substring(0, raw.length() - 1));
        if (value < 0L) throw new IllegalArgumentException("Negative duration is not supported.");
        return switch (raw.charAt(raw.length() - 1)) {
            case 'm' -> EchoDuration.minutes(value);
            case 'h' -> EchoDuration.hours(value);
            default -> throw new IllegalArgumentException("Duration unit must be m or h.");
        };
    }
}
