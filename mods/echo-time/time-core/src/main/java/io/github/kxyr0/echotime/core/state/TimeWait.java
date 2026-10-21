package io.github.kxyr0.echotime.core.state;

import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import io.github.kxyr0.echotime.api.value.TimeOfDay;
import java.util.Objects;

public record TimeWait(TimeWaitId waitId, TimeOfDay requestedTime, long targetAbsoluteMicroseconds) {
    public TimeWait {
        waitId = Objects.requireNonNull(waitId, "waitId");
        requestedTime = Objects.requireNonNull(requestedTime, "requestedTime");
        if (targetAbsoluteMicroseconds < 0L) {
            throw new IllegalArgumentException("Wait target cannot be negative.");
        }
    }
}
