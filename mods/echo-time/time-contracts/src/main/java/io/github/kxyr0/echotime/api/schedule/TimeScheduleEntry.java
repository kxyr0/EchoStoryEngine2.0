package io.github.kxyr0.echotime.api.schedule;

import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import java.util.Objects;
import java.util.Optional;

public record TimeScheduleEntry(
        String selectorId,
        TimeOfDay time,
        Optional<EchoDay> day,
        String generation,
        int priority) {
    public TimeScheduleEntry {
        selectorId = require(selectorId, "selectorId");
        time = Objects.requireNonNull(time, "time");
        day = Objects.requireNonNull(day, "day");
        generation = require(generation, "generation");
    }

    public boolean daily() {
        return day.isEmpty();
    }

    private static String require(String value, String name) {
        String checked = Objects.requireNonNull(value, name).trim();
        if (checked.isEmpty()) throw new IllegalArgumentException(name + " cannot be blank.");
        return checked;
    }
}
