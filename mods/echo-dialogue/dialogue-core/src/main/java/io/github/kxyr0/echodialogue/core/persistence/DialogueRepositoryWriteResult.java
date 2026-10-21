package io.github.kxyr0.echodialogue.core.persistence;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit repository write, acknowledgement or cleanup result.
 */
public record DialogueRepositoryWriteResult(
        Kind kind,
        Optional<DialogueCheckpoint> checkpoint,
        List<DialogueDiagnostic> diagnostics,
        int affectedRecords
) {
    public DialogueRepositoryWriteResult {
        kind = Objects.requireNonNull(kind, "kind");
        checkpoint = Objects.requireNonNull(checkpoint, "checkpoint");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        if (affectedRecords < 0) {
            throw new IllegalArgumentException("Affected records must not be negative.");
        }
    }

    public static DialogueRepositoryWriteResult saved(Kind kind, DialogueCheckpoint checkpoint) {
        return new DialogueRepositoryWriteResult(kind, Optional.of(checkpoint), List.of(), 1);
    }

    public static DialogueRepositoryWriteResult completed(int affectedRecords, List<DialogueDiagnostic> diagnostics) {
        return new DialogueRepositoryWriteResult(Kind.COMPLETED, Optional.empty(), diagnostics, affectedRecords);
    }

    public static DialogueRepositoryWriteResult conflict(List<DialogueDiagnostic> diagnostics) {
        return new DialogueRepositoryWriteResult(Kind.REVISION_CONFLICT, Optional.empty(), diagnostics, 0);
    }

    public static DialogueRepositoryWriteResult notFound() {
        return new DialogueRepositoryWriteResult(Kind.NOT_FOUND, Optional.empty(), List.of(), 0);
    }

    public static DialogueRepositoryWriteResult failed(List<DialogueDiagnostic> diagnostics) {
        return new DialogueRepositoryWriteResult(Kind.FAILED, Optional.empty(), diagnostics, 0);
    }

    public enum Kind {
        CREATED,
        UPDATED,
        COMPLETED,
        NOT_FOUND,
        REVISION_CONFLICT,
        QUEUE_SATURATED,
        SHUTDOWN,
        FAILED
    }
}
