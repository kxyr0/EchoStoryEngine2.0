package io.github.kxyr0.echodialogue.api.service;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Public result of a session query.
 */
public record DialogueQueryResult(
        DialogueOperationStatus status,
        Optional<DialogueSessionView> session,
        List<DialogueDiagnostic> diagnostics
) {
    public DialogueQueryResult {
        status = Objects.requireNonNull(status, "status");
        session = Objects.requireNonNull(session, "session");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
