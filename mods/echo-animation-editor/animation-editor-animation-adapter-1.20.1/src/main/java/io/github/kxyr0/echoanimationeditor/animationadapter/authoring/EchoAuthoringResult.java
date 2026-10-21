package io.github.kxyr0.echoanimationeditor.animationadapter.authoring;

import java.util.Objects;
import java.util.Optional;

public record EchoAuthoringResult(boolean success, Optional<EchoAuthoringDraftHandle> draft, String message) {
    public EchoAuthoringResult {
        draft = Objects.requireNonNull(draft, "draft");
        message = Objects.requireNonNull(message, "message");
    }

    public static EchoAuthoringResult opened(EchoAuthoringDraftHandle draft, String message) {
        return new EchoAuthoringResult(true, Optional.of(draft), message);
    }

    public static EchoAuthoringResult failure(String message) {
        return new EchoAuthoringResult(false, Optional.empty(), message == null ? "" : message);
    }
}
