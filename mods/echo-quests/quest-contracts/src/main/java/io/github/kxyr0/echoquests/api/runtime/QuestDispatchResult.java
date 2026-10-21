package io.github.kxyr0.echoquests.api.runtime;

import java.util.Objects;
import java.util.Optional;

public record QuestDispatchResult(Status status, Optional<QuestProgress> progress, String detail) {
    public QuestDispatchResult {
        status = Objects.requireNonNull(status, "status");
        progress = Objects.requireNonNull(progress, "progress");
        detail = Objects.requireNonNull(detail, "detail");
    }

    public enum Status {
        TRANSITIONED,
        COMPLETED,
        FAILED,
        NO_TRANSITION,
        NOT_STARTED,
        NOT_ACTIVE,
        UNKNOWN_QUEST
    }
}
