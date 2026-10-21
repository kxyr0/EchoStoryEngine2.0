package io.github.kxyr0.echotime.forge.api;

import java.util.Objects;

public record EchoTimeServiceBridge(
        EchoTimeQueryService query,
        EchoTimeMutationService mutations,
        EchoTimeScheduleService schedule,
        EchoTimeWaitService waits) {
    public EchoTimeServiceBridge {
        query = Objects.requireNonNull(query, "query");
        mutations = Objects.requireNonNull(mutations, "mutations");
        schedule = Objects.requireNonNull(schedule, "schedule");
        waits = Objects.requireNonNull(waits, "waits");
    }
}
