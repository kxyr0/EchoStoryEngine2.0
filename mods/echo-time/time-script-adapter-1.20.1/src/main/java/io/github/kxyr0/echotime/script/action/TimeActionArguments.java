package io.github.kxyr0.echotime.script.action;

import io.github.kxyr0.echotime.api.mutation.TimeCrossingPolicy;
import io.github.kxyr0.echotime.api.value.EchoDuration;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import java.util.Map;
import java.util.Optional;

final class TimeActionArguments {
    private final Map<String, String> values;

    TimeActionArguments(Map<String, String> values) { this.values = Map.copyOf(values); }

    TimeOfDay time(String name) { return TimeOfDay.parse(required(name)); }

    Optional<io.github.kxyr0.echotime.api.value.EchoDay> day() {
        return Optional.ofNullable(values.get("day")).map(Long::parseLong)
                .map(io.github.kxyr0.echotime.api.value.EchoDay::new);
    }

    EchoDuration duration() {
        long amount = Long.parseLong(required("durationValue"));
        return switch (required("durationUnit")) {
            case "Minutes" -> EchoDuration.minutes(amount);
            case "Hours" -> EchoDuration.hours(amount);
            default -> throw new IllegalArgumentException("Unsupported duration unit.");
        };
    }

    TimeCrossingPolicy crossing(TimeCrossingPolicy fallback) {
        String value = values.getOrDefault("crossingPolicy", fallback.name());
        return TimeCrossingPolicy.valueOf(value);
    }

    private String required(String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing Time argument: " + name);
        return value;
    }
}
