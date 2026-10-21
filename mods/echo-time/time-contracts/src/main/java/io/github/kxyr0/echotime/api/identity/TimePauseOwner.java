package io.github.kxyr0.echotime.api.identity;

import java.util.Objects;

public record TimePauseOwner(String value) implements Comparable<TimePauseOwner> {
    public TimePauseOwner {
        value = Objects.requireNonNull(value, "value").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Time pause owner cannot be blank.");
    }

    @Override
    public int compareTo(TimePauseOwner other) {
        return value.compareTo(other.value);
    }
}
