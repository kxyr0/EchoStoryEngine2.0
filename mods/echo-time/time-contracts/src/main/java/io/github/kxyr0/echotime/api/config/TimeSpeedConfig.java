package io.github.kxyr0.echotime.api.config;

import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import io.github.kxyr0.echotime.api.value.TimeRate;
import java.util.List;
import java.util.Objects;

public record TimeSpeedConfig(
        String id,
        EchoDay initialDay,
        TimeOfDay initialTime,
        TimeRate rate,
        boolean running,
        boolean syncSun,
        boolean syncMoon,
        List<String> sunDimensions,
        long revision,
        int maximumCrossedEvents) {
    public TimeSpeedConfig {
        id = Objects.requireNonNull(id, "id").trim();
        if (id.isEmpty()) throw new IllegalArgumentException("Config ID cannot be blank.");
        initialDay = Objects.requireNonNull(initialDay, "initialDay");
        initialTime = Objects.requireNonNull(initialTime, "initialTime");
        rate = Objects.requireNonNull(rate, "rate");
        sunDimensions = List.copyOf(Objects.requireNonNull(sunDimensions, "sunDimensions"));
        if (revision < 0L) throw new IllegalArgumentException("Config revision cannot be negative.");
        if (maximumCrossedEvents <= 0) throw new IllegalArgumentException("Crossing limit must be positive.");
    }
}
