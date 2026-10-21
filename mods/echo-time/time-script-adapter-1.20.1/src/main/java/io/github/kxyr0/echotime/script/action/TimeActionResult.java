package io.github.kxyr0.echotime.script.action;

import io.github.kxyr0.echotime.api.identity.TimeWaitId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record TimeActionResult(
        Status status,
        String detail,
        Map<String, String> outputs,
        Optional<TimeWaitId> waitId) {
    public TimeActionResult {
        status = Objects.requireNonNull(status, "status");
        detail = Objects.requireNonNull(detail, "detail");
        outputs = Map.copyOf(Objects.requireNonNull(outputs, "outputs"));
        waitId = Objects.requireNonNull(waitId, "waitId");
    }

    public static TimeActionResult success(String detail, Map<String, String> outputs) {
        return new TimeActionResult(Status.SUCCESS, detail, outputs, Optional.empty());
    }

    public static TimeActionResult failure(String detail) {
        return new TimeActionResult(Status.FAILURE, detail, Map.of(), Optional.empty());
    }

    public static TimeActionResult waiting(TimeWaitId waitId) {
        return new TimeActionResult(Status.WAITING, "waiting", Map.of(), Optional.of(waitId));
    }

    public enum Status { SUCCESS, FAILURE, WAITING }
}
