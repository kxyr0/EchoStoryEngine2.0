package io.github.kxyr0.echoanimationauthoring.api.service;

import java.util.Objects;

public record AuthoringHotReloadResult(Status status, String message) {
    public AuthoringHotReloadResult {
        status = Objects.requireNonNull(status, "status");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        SCHEDULED_BY_FILE_CHANGE,
        DRAFT_NOT_FOUND,
        NOT_SAVED,
        FAILURE
    }
}
