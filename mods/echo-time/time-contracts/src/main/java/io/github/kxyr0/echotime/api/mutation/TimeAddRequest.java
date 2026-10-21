package io.github.kxyr0.echotime.api.mutation;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.value.EchoDuration;
import java.util.Objects;

public record TimeAddRequest(
        TimeMutationId mutationId,
        EchoDuration duration,
        TimeCrossingPolicy crossingPolicy,
        TimeRequestSource source) {
    public TimeAddRequest {
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
        duration = Objects.requireNonNull(duration, "duration");
        crossingPolicy = Objects.requireNonNull(crossingPolicy, "crossingPolicy");
        source = Objects.requireNonNull(source, "source");
    }

    public String signature() {
        return "ADD|" + duration.microseconds() + "|" + crossingPolicy;
    }
}
