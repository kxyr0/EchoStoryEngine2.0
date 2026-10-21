package io.github.kxyr0.echodialogue.core.session;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * New immutable state plus optional presentation event and diagnostics.
 */
public record DialogueTransition(
        DialogueSessionState state,
        DialogueTransitionStatus status,
        Optional<DialoguePresentation> presentation,
        List<DialogueDiagnostic> diagnostics,
        Optional<DialogueId> targetDialogueId,
        Optional<io.github.kxyr0.echodialogue.api.choice.ChoiceId> selectedChoice
) {
    public DialogueTransition(DialogueSessionState state, DialogueTransitionStatus status,
            Optional<DialoguePresentation> presentation, List<DialogueDiagnostic> diagnostics,
            Optional<DialogueId> targetDialogueId) {
        this(state, status, presentation, diagnostics, targetDialogueId, Optional.empty());
    }
    public DialogueTransition withSelectedChoice(io.github.kxyr0.echodialogue.api.choice.ChoiceId choice) {
        return new DialogueTransition(state, status, presentation, diagnostics, targetDialogueId, Optional.of(choice));
    }
    public DialogueTransition {
        state = Objects.requireNonNull(state, "state");
        status = Objects.requireNonNull(status, "status");
        presentation = Objects.requireNonNull(presentation, "presentation");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        targetDialogueId = Objects.requireNonNull(targetDialogueId, "targetDialogueId");
        selectedChoice = Objects.requireNonNull(selectedChoice, "selectedChoice");
    }

    public DialogueTransition(
            DialogueSessionState state,
            DialogueTransitionStatus status,
            Optional<DialoguePresentation> presentation,
            List<DialogueDiagnostic> diagnostics
    ) {
        this(state, status, presentation, diagnostics, Optional.empty());
    }

    public static DialogueTransition of(
            DialogueSessionState state,
            DialogueTransitionStatus status,
            Optional<DialoguePresentation> presentation,
            DialogueDiagnostic diagnostic
    ) {
        return new DialogueTransition(state, status, presentation, List.of(diagnostic));
    }

    public static DialogueTransition loadRequested(DialogueSessionState state, DialogueId targetDialogueId) {
        return new DialogueTransition(state, DialogueTransitionStatus.LOAD_REQUESTED, Optional.empty(), List.of(),
                Optional.of(targetDialogueId));
    }
}
