package io.github.kxyr0.echotime.core.schedule;

import io.github.kxyr0.echotime.api.schedule.TimeOccurrenceCrossed;
import java.util.List;
import java.util.Objects;

public record ScheduleCrossingResult(List<TimeOccurrenceCrossed> occurrences, boolean limitExceeded) {
    public ScheduleCrossingResult {
        occurrences = List.copyOf(Objects.requireNonNull(occurrences, "occurrences"));
        if (limitExceeded && !occurrences.isEmpty()) {
            throw new IllegalArgumentException("A rejected crossing cannot expose a partial occurrence subset.");
        }
    }

    public static ScheduleCrossingResult exceeded() {
        return new ScheduleCrossingResult(List.of(), true);
    }
}
