package io.github.kxyr0.echoanimationauthoring.api.service;

import io.github.kxyr0.echoanimationauthoring.api.draft.AuthoringDraftView;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public record AuthoringOperationResult(
        Status status,
        Optional<AuthoringDraftView> draft,
        Optional<Path> path,
        String message
) {
    public AuthoringOperationResult {
        status = Objects.requireNonNull(status, "status");
        draft = Objects.requireNonNull(draft, "draft");
        path = Objects.requireNonNull(path, "path");
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        UNDONE,
        REDONE,
        NOTHING_TO_UNDO,
        NOTHING_TO_REDO,
        AUTOSAVED,
        RECOVERED,
        PREVIEW_APPLIED,
        PREVIEW_CLEARED,
        DRAFT_NOT_FOUND,
        FAILURE
    }
}
