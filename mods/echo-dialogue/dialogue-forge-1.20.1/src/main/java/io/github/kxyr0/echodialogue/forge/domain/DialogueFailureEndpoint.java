package io.github.kxyr0.echodialogue.forge.domain;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.failure.DialogueFailureRequest;
import io.github.kxyr0.echodialogue.api.failure.DialogueFailureResult;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWaitResult;
import io.github.kxyr0.echodialogue.core.host.ActiveDialogueSessions;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class DialogueFailureEndpoint {
    private final ActiveDialogueSessions sessions;
    private final Function<DialogueTransition, DialogueExternalWaitResult> save;
    private final Function<DialogueSessionState, DialogueSessionView> views;

    public DialogueFailureEndpoint(ActiveDialogueSessions sessions,
            Function<DialogueTransition, DialogueExternalWaitResult> save,
            Function<DialogueSessionState, DialogueSessionView> views) {
        this.sessions = sessions;
        this.save = save;
        this.views = views;
    }

    public DialogueFailureResult fail(DialogueFailureRequest request) {
        var state = sessions.stateBySessionId(request.sessionId());
        if (state.isEmpty()) {
            return new DialogueFailureResult(DialogueOperationStatus.NOT_FOUND, Optional.empty(), List.of());
        }
        var current = state.get();
        if (current.revision() != request.expectedRevision() || current.phase() == DialogueSessionPhase.COMPLETED
                || current.phase() == DialogueSessionPhase.CANCELLED
                || current.phase() == DialogueSessionPhase.FAILED) {
            return new DialogueFailureResult(DialogueOperationStatus.REJECTED, Optional.of(views.apply(current)),
                    List.of());
        }
        var failed = new DialogueSessionState(current.sessionId(), current.dialogueId(), current.audienceId(),
                current.currentNodeId(), current.lineOrdinal(), current.presentationKind(), DialogueSessionPhase.FAILED,
                current.revision() + 1, Optional.empty(), Optional.empty());
        var diagnostic = DialogueDiagnostic.error(request.code(), DiagnosticCategory.COMMAND_FAILURE,
                "Исполнение диалога завершилось ошибкой интеграции.");
        var result = save.apply(DialogueTransition.of(failed, DialogueTransitionStatus.FAILED,
                Optional.empty(), diagnostic));
        return new DialogueFailureResult(result.status(), result.session(), result.diagnostics());
    }
}
