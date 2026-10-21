package io.github.kxyr0.echoquests.api.runtime;

import java.util.Objects;
import java.util.Optional;

public record QuestStartResult(Status status, Optional<QuestProgress> progress, String detail) {
    public QuestStartResult {
        status = Objects.requireNonNull(status, "status");
        progress = Objects.requireNonNull(progress, "progress");
        detail = Objects.requireNonNull(detail, "detail");
    }

    public enum Status {
        STARTED,
        ALREADY_ACTIVE,
        ALREADY_TERMINAL,
        UNKNOWN_QUEST
    }
}
