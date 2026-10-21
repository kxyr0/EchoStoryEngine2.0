package io.github.kxyr0.echotime.core.mutation;

import io.github.kxyr0.echotime.api.mutation.TimeMutationResult;
import io.github.kxyr0.echotime.api.schedule.TimeOccurrenceCrossed;
import java.util.List;
import java.util.Objects;

public record MutationExecution(TimeMutationResult result, List<TimeOccurrenceCrossed> occurrences) {
    public MutationExecution {
        result = Objects.requireNonNull(result, "result");
        occurrences = List.copyOf(Objects.requireNonNull(occurrences, "occurrences"));
    }
}
