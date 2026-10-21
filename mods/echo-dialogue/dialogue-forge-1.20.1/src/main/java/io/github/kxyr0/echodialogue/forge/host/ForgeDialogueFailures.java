package io.github.kxyr0.echodialogue.forge.host;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import java.util.Optional;
import java.util.UUID;

/**
 * Creates non-durable command/host failure transitions.
 */
final class ForgeDialogueFailures {
    private ForgeDialogueFailures() {
    }

    static DialogueTransition missing(DialogueId dialogueId, AudienceId audienceId) {
        DialogueDiagnostic diagnostic = DialogueDiagnostic.error("EDLG-FORGE-005",
                DiagnosticCategory.COMMAND_FAILURE, "Диалог не найден: " + dialogueId);
        return transition("missing-", dialogueId, audienceId, diagnostic);
    }

    static DialogueTransition stopped(DialogueId dialogueId, AudienceId audienceId) {
        DialogueDiagnostic diagnostic = DialogueDiagnostic.error("EDLG-FORGE-008",
                DiagnosticCategory.HOST_LIFECYCLE, "Dialogue host уже остановлен; запрос отклонён.");
        return transition("stopped-", dialogueId, audienceId, diagnostic);
    }

    private static DialogueTransition transition(
            String prefix,
            DialogueId dialogueId,
            AudienceId audienceId,
            DialogueDiagnostic diagnostic
    ) {
        DialogueSessionState state = new DialogueSessionState(
                new DialogueSessionId(prefix + UUID.randomUUID()),
                dialogueId,
                audienceId,
                new DialogueNodeId("failed"),
                0,
                DialoguePresentationKind.LINE,
                DialogueSessionPhase.FAILED,
                0,
                Optional.empty());
        return DialogueTransition.of(state, DialogueTransitionStatus.FAILED, Optional.empty(), diagnostic);
    }
}
