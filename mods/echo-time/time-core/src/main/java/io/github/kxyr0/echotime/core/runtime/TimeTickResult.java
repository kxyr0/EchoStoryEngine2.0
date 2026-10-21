package io.github.kxyr0.echotime.core.runtime;

import io.github.kxyr0.echotime.api.schedule.TimeOccurrenceCrossed;
import io.github.kxyr0.echotime.core.clock.ClockTransition;
import io.github.kxyr0.echotime.core.state.TimeWait;
import java.util.List;
import java.util.Objects;

public record TimeTickResult(
        ClockTransition transition,
        List<TimeOccurrenceCrossed> occurrences,
        List<TimeWait> completedWaits,
        boolean crossingLimitExceeded) {
    public TimeTickResult {
        transition = Objects.requireNonNull(transition, "transition");
        occurrences = List.copyOf(Objects.requireNonNull(occurrences, "occurrences"));
        completedWaits = List.copyOf(Objects.requireNonNull(completedWaits, "completedWaits"));
    }
}
