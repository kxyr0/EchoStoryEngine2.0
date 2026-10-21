package io.github.kxyr0.echodialogue.api.service;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Public result of a session cancellation request.
 */
public record DialogueCancelResult(
        DialogueOperationStatus status,
        Optional<DialogueSessionView> session,
        List<DialogueDiagnostic> diagnostics
) {
    public DialogueCancelResult {
        status = Objects.requireNonNull(status, "status");
        session = Objects.requireNonNull(session, "session");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
