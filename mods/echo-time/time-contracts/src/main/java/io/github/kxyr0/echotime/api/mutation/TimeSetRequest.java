package io.github.kxyr0.echotime.api.mutation;

import io.github.kxyr0.echotime.api.identity.TimeMutationId;
import io.github.kxyr0.echotime.api.value.EchoDay;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import java.util.Objects;
import java.util.Optional;

public record TimeSetRequest(
        TimeMutationId mutationId,
        Optional<EchoDay> day,
        TimeOfDay time,
        TimeCrossingPolicy crossingPolicy,
        TimeRequestSource source) {
    public TimeSetRequest {
        mutationId = Objects.requireNonNull(mutationId, "mutationId");
        day = Objects.requireNonNull(day, "day");
        time = Objects.requireNonNull(time, "time");
        crossingPolicy = Objects.requireNonNull(crossingPolicy, "crossingPolicy");
        source = Objects.requireNonNull(source, "source");
    }

    public String signature() {
        return "SET|" + day.map(value -> Long.toString(value.value())).orElse("CURRENT")
                + "|" + time.microseconds() + "|" + crossingPolicy;
    }
}
