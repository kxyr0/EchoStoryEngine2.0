package io.github.kxyr0.echoanimationauthoring.api.service;

import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftView;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public record AuthoringDraftSaveResult(
        Status status,
        Optional<AuthoringDraftView> draft,
        Optional<Path> sourcePath,
        boolean parserPass,
        boolean binderPass,
        boolean compilerPass,
        String message
) {
    public AuthoringDraftSaveResult {
        status = Objects.requireNonNull(status, "status");
        draft = Objects.requireNonNull(draft, "draft");
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        SAVED,
        DRAFT_NOT_FOUND,
        VALIDATION_FAILED,
        STALE_SOURCE,
        FAILURE
    }
}
