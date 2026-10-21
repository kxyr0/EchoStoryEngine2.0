package io.github.kxyr0.echodialogue.persistence.recovery;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Validated recovery decision for one durable checkpoint.
 */
public record DialogueRecoveryPlan(
        Kind kind,
        Optional<DialogueCheckpoint> checkpoint,
        List<DialogueDiagnostic> diagnostics
) {
    public DialogueRecoveryPlan {
        kind = Objects.requireNonNull(kind, "kind");
        checkpoint = Objects.requireNonNull(checkpoint, "checkpoint");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public static DialogueRecoveryPlan restored(Kind kind, DialogueCheckpoint checkpoint) {
        return new DialogueRecoveryPlan(kind, Optional.of(checkpoint), List.of());
    }

    public static DialogueRecoveryPlan blocked(DialogueDiagnostic diagnostic, DialogueCheckpoint checkpoint) {
        return new DialogueRecoveryPlan(Kind.BLOCKED, Optional.of(checkpoint), List.of(diagnostic));
    }

    public static DialogueRecoveryPlan corrupt(DialogueDiagnostic diagnostic) {
        return new DialogueRecoveryPlan(Kind.CORRUPT, Optional.empty(), List.of(diagnostic));
    }

    public enum Kind {
        ACTIVE_DETACHED,
        TERMINAL,
        BLOCKED,
        CORRUPT
    }
}
