package io.github.kxyr0.echotime.api.wait;

import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import java.util.Objects;

public record TimeWaitResult(
        TimeWaitId waitId,
        TimeWaitStatus status,
        TimeOfDay requestedTime,
        long targetAbsoluteMicroseconds) {
    public TimeWaitResult {
        waitId = Objects.requireNonNull(waitId, "waitId");
        status = Objects.requireNonNull(status, "status");
        requestedTime = Objects.requireNonNull(requestedTime, "requestedTime");
        if (targetAbsoluteMicroseconds < 0L) throw new IllegalArgumentException("Wait target cannot be negative.");
    }
}
