package io.github.kxyr0.echoscene.api.command;

import java.util.Objects;

public record SceneCommandStart(Status status, String detail) {
    public SceneCommandStart {
        status = Objects.requireNonNull(status, "status");
        detail = Objects.requireNonNull(detail, "detail");
    }

    public static SceneCommandStart completed() {
        return new SceneCommandStart(Status.COMPLETED, "");
    }

    public static SceneCommandStart waiting(String handle) {
        return new SceneCommandStart(Status.WAITING, handle);
    }

    public static SceneCommandStart failed(String detail) {
        return new SceneCommandStart(Status.FAILED, detail);
    }

    public enum Status {
        COMPLETED,
        WAITING,
        FAILED
    }
}
