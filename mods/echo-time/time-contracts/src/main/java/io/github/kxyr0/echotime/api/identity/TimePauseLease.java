package io.github.kxyr0.echotime.api.identity;

import java.util.Objects;

public record TimePauseLease(TimePauseOwner owner, TimeMutationId acquiredBy) {
    public TimePauseLease {
        owner = Objects.requireNonNull(owner, "owner");
        acquiredBy = Objects.requireNonNull(acquiredBy, "acquiredBy");
    }
}
