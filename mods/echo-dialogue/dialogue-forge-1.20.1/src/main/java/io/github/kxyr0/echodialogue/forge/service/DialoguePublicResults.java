package io.github.kxyr0.echodialogue.forge.service;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.service.DialogueOperationStatus;
import io.github.kxyr0.echodialogue.api.service.DialogueQueryResult;
import io.github.kxyr0.echodialogue.api.service.DialogueStartRequest;
import io.github.kxyr0.echodialogue.api.service.DialogueStartResult;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWaitResult;
import java.util.List;
import java.util.Optional;

public final class DialoguePublicResults {
    private DialoguePublicResults() {
    }

    public static DialogueQueryResult query(Optional<DialogueSessionView> view) {
        return view.map(value -> new DialogueQueryResult(status(value), view, List.of()))
                .orElseGet(() -> new DialogueQueryResult(DialogueOperationStatus.NOT_FOUND, Optional.empty(),
                        List.of()));
    }

    public static DialogueStartResult existingStart(DialogueStartRequest request, DialogueSessionView view) {
        if (!view.dialogueId().equals(request.dialogueId()) || !view.audienceId().equals(request.audienceId())) {
            return new DialogueStartResult(DialogueOperationStatus.CONFLICT, Optional.of(view), List.of());
        }
        return new DialogueStartResult(status(view), Optional.of(view), List.of());
    }

    public static DialogueExternalWaitResult external(
            DialogueOperationStatus status,
            Optional<DialogueSessionView> view,
            List<DialogueDiagnostic> diagnostics
    ) {
        return new DialogueExternalWaitResult(status, view, diagnostics,
                view.flatMap(DialogueSessionView::externalWait));
    }

    public static DialogueExternalWaitResult externalMissing(Optional<DialogueSessionView> view) {
        return view.map(value -> external(status(value), view, List.of()))
                .orElseGet(() -> external(DialogueOperationStatus.NOT_FOUND, Optional.empty(), List.of()));
    }

    public static DialogueOperationStatus status(DialogueSessionView view) {
        return switch (view.status()) {
            case ACTIVE -> DialogueOperationStatus.ALREADY_ACTIVE;
            case WAITING -> DialogueOperationStatus.WAITING;
            case COMPLETED, CANCELLED, FAILED -> DialogueOperationStatus.ALREADY_TERMINAL;
            case BLOCKED -> DialogueOperationStatus.BLOCKED;
        };
    }

    public static boolean activeOrWaiting(DialogueSessionView view) {
        return status(view) == DialogueOperationStatus.ALREADY_ACTIVE
                || status(view) == DialogueOperationStatus.WAITING;
    }

    public static DialogueDiagnostic missingDiagnostic(DialogueId dialogueId) {
        return DialogueDiagnostic.error("EDLG-FORGE-021", DiagnosticCategory.COMPILED_DIALOGUE_MISSING,
                "Compiled dialogue for public start not found: " + dialogueId + ".");
    }
}
