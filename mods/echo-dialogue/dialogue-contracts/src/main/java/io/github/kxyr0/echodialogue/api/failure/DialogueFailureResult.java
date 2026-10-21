package io.github.kxyr0.echodialogue.api.failure;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import java.util.List;
import java.util.Optional;

public record DialogueFailureResult(DialogueOperationStatus status, Optional<DialogueSessionView> session,
        List<DialogueDiagnostic> diagnostics) {
    public DialogueFailureResult {
        java.util.Objects.requireNonNull(status, "status");
        java.util.Objects.requireNonNull(session, "session");
        diagnostics = List.copyOf(diagnostics);
    }
    public static DialogueFailureResult unavailable() {
        return new DialogueFailureResult(DialogueOperationStatus.SERVICE_UNAVAILABLE, Optional.empty(), List.of());
    }
}
