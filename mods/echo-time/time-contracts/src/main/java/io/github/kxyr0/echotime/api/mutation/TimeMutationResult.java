package io.github.kxyr0.echotime.api.mutation;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.value.EchoInstant;
import java.util.Objects;

public record TimeMutationResult(
        TimeMutationId mutationId,
        TimeMutationStatus status,
        EchoInstant previous,
        EchoInstant current,
        TimeCrossingPolicy crossingPolicy,
        String message) {
    public TimeMutationResult {
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
        status = Objects.requireNonNull(status, "status");
        previous = Objects.requireNonNull(previous, "previous");
        current = Objects.requireNonNull(current, "current");
        crossingPolicy = Objects.requireNonNull(crossingPolicy, "crossingPolicy");
        message = Objects.requireNonNull(message, "message");
    }

    public boolean applied() {
        return status == TimeMutationStatus.APPLIED;
    }
}
