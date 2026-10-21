package io.github.kxyr0.echodialogue.api.wait;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record DialogueExternalWaitResult(
        DialogueOperationStatus status,
        Optional<DialogueSessionView> session,
        List<DialogueDiagnostic> diagnostics,
        Optional<DialogueExternalWait> externalWait
) {
    public DialogueExternalWaitResult {
        status = Objects.requireNonNull(status, "status");
        session = Objects.requireNonNull(session, "session");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        externalWait = Objects.requireNonNull(externalWait, "externalWait");
    }
}
