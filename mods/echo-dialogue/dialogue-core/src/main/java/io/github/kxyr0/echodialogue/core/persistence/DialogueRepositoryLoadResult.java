package io.github.kxyr0.echodialogue.core.persistence;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Explicit repository load result.
 */
public record DialogueRepositoryLoadResult(
        Kind kind,
        Optional<DialogueCheckpoint> checkpoint,
        List<DialogueDiagnostic> diagnostics
) {
    public DialogueRepositoryLoadResult {
        kind = Objects.requireNonNull(kind, "kind");
        checkpoint = Objects.requireNonNull(checkpoint, "checkpoint");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public static DialogueRepositoryLoadResult found(
            DialogueCheckpoint checkpoint,
            List<DialogueDiagnostic> diagnostics
    ) {
        return new DialogueRepositoryLoadResult(Kind.FOUND, Optional.of(checkpoint), diagnostics);
    }

    public static DialogueRepositoryLoadResult notFound() {
        return new DialogueRepositoryLoadResult(Kind.NOT_FOUND, Optional.empty(), List.of());
    }

    public static DialogueRepositoryLoadResult failed(List<DialogueDiagnostic> diagnostics) {
        return new DialogueRepositoryLoadResult(Kind.FAILED, Optional.empty(), diagnostics);
    }

    public enum Kind {
        FOUND,
        NOT_FOUND,
        FAILED
    }
}
