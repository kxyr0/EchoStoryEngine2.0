package io.github.kxyr0.echodialogue.core.session.waiting;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import java.util.List;
import java.util.Optional;

public final class DialogueExternalWaitTransitions {
    private DialogueExternalWaitTransitions() {
    }

    public static DialogueTransition waitExternal(
            CompiledDialogue dialogue,
            DialogueSessionState state,
            int expectedRevision,
            DialogueExternalWait wait
    ) {
        if (state.revision() != expectedRevision) { return revisionRejected(state, expectedRevision); }
        if (state.phase() != DialogueSessionPhase.ACTIVE) {
            return invalidInput(state, "External wait can only start from ACTIVE session state.");
        }
        if (dialogue.findNode(wait.continuationNodeId()).isEmpty()) {
            return invalidInput(state, "External wait continuation node does not belong to this dialogue.");
        }
        return new DialogueTransition(state.waiting(wait), DialogueTransitionStatus.WAITING, Optional.empty(),
                List.of());
    }

    public static DialogueTransition resumeExternal(
            DialogueSessionState state,
            int expectedRevision,
            String waitKey
    ) {
        if (state.revision() != expectedRevision) { return revisionRejected(state, expectedRevision); }
        if (state.phase() != DialogueSessionPhase.WAITING || state.externalWait().isEmpty()) {
            return invalidInput(state, "Session is not waiting for an external integration.");
        }
        if (!state.externalWait().orElseThrow().waitKey().equals(waitKey)) {
            return invalidInput(state, "External wait key does not match current session wait.");
        }
        return new DialogueTransition(state.resumed(), DialogueTransitionStatus.RESUMED, Optional.empty(), List.of());
    }

    private static DialogueTransition revisionRejected(DialogueSessionState state, int expectedRevision) {
        DiagnosticCategory category = expectedRevision < state.revision()
                ? DiagnosticCategory.DUPLICATE_ADVANCE_REQUEST
                : DiagnosticCategory.STALE_ADVANCE_REQUEST;
        DialogueDiagnostic diagnostic = DialogueDiagnostic.error("EDLG-CORE-004",
                category, "Client revision does not match the server session revision.");
        return DialogueTransition.of(state, DialogueTransitionStatus.REJECTED, Optional.empty(), diagnostic);
    }

    private static DialogueTransition invalidInput(DialogueSessionState state, String message) {
        DialogueDiagnostic diagnostic = DialogueDiagnostic.error("EDLG-CORE-006",
                DiagnosticCategory.INVALID_DIALOGUE_INPUT, message);
        return DialogueTransition.of(state, DialogueTransitionStatus.REJECTED, Optional.empty(), diagnostic);
    }
}
