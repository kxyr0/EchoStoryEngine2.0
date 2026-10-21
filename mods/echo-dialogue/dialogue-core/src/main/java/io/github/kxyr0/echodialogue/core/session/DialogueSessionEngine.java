package io.github.kxyr0.echodialogue.core.session;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueChoice;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.session.command.DialogueCommandResume;
import io.github.kxyr0.echodialogue.core.session.command.DialogueCommandWaits;
import io.github.kxyr0.echodialogue.core.session.waiting.DialogueExternalWaitTransitions;
import io.github.kxyr0.echodialogue.core.visual.DialoguePresentationFactory;
import java.util.List;
import java.util.Optional;

public final class DialogueSessionEngine {
    public DialogueTransition start(CompiledDialogue dialogue, DialogueSessionId sessionId, AudienceId audienceId) {
        return firstPresentation(new PresentationSeed(dialogue, sessionId, audienceId, dialogue.startNode()), 0, 0,
                DialogueTransitionStatus.STARTED);
    }

    public DialogueTransition load(CompiledDialogue dialogue, DialogueSessionState previousState) {
        if (previousState.phase() != DialogueSessionPhase.ACTIVE) { return terminal(previousState); }
        return firstPresentation(new PresentationSeed(dialogue, previousState.sessionId(), previousState.audienceId(),
                dialogue.startNode()), 0, previousState.revision() + 1, DialogueTransitionStatus.LOADED);
    }

    public DialogueTransition apply(CompiledDialogue dialogue, DialogueSessionState state, DialogueInput input) {
        if (!state.sessionId().equals(input.sessionId())) {
            return reject(state, "EDLG-CORE-001", DiagnosticCategory.WRONG_PLAYER_REQUEST,
                    "Request belongs to another dialogue session.");
        }
        if (state.revision() != input.expectedRevision()) { return revisionRejected(state, input.expectedRevision()); }
        if (input.kind() == DialogueInputKind.CANCEL) { return cancel(state); }
        if (state.phase() == DialogueSessionPhase.WAITING) {
            return invalidInput(state, "Session is waiting for an external integration.");
        }
        if (state.phase() != DialogueSessionPhase.ACTIVE) { return terminal(state); }
        if (input.kind() == DialogueInputKind.SELECT_CHOICE) { return selectChoice(dialogue, state, input); }
        return advance(dialogue, state);
    }

    public DialogueTransition waitExternal(CompiledDialogue dialogue, DialogueSessionState state,
            int expectedRevision, DialogueExternalWait wait) {
        return DialogueExternalWaitTransitions.waitExternal(dialogue, state, expectedRevision, wait);
    }

    public DialogueTransition resumeExternal(CompiledDialogue dialogue, DialogueSessionState state,
            int expectedRevision, String waitKey) {
        return DialogueCommandResume.resume(dialogue, state, expectedRevision, waitKey);
    }

    private DialogueTransition advance(CompiledDialogue dialogue, DialogueSessionState state) {
        if (state.presentationKind() != DialoguePresentationKind.LINE) {
            return invalidInput(state, "Advance is not available during choice presentation.");
        }
        CompiledDialogueNode node = dialogue.findNode(state.currentNodeId()).orElseThrow();
        int nextPosition = state.lineOrdinal() + 1;
        Optional<DialogueExternalWait> wait = DialogueCommandWaits.waitFor(state, node, nextPosition);
        if (wait.isPresent()) {
            return new DialogueTransition(state.waiting(wait.orElseThrow()), DialogueTransitionStatus.WAITING,
                    Optional.empty(), List.of());
        }
        if (nextPosition < node.lines().size()) {
            return present(dialogue, state.withPresentation(
                    node.id(), nextPosition, DialoguePresentationKind.LINE));
        }
        if (node.hasChoices()) {
            return present(dialogue, state.withPresentation(node.id(), state.lineOrdinal(),
                    DialoguePresentationKind.CHOICE));
        }
        if (node.hasTransition()) { return transition(dialogue, state, node); }
        return complete(state, node);
    }

    private DialogueTransition selectChoice(CompiledDialogue dialogue, DialogueSessionState state,
            DialogueInput input) {
        if (state.presentationKind() != DialoguePresentationKind.CHOICE) {
            return invalidInput(state, "ChoiceId cannot be selected during line presentation.");
        }
        CompiledDialogueNode node = dialogue.findNode(state.currentNodeId()).orElseThrow();
        Optional<CompiledDialogueChoice> choice = node.choices().stream()
                .filter(value -> value.id().equals(input.choiceId().orElseThrow()))
                .findFirst();
        if (choice.isEmpty()) { return unknownChoice(state); }
        CompiledDialogueNode target = dialogue.findNode(choice.orElseThrow().targetNodeId()).orElseThrow();
        return firstPresentation(dialogue, state, target).withSelectedChoice(choice.orElseThrow().id());
    }

    private DialogueTransition firstPresentation(CompiledDialogue dialogue, DialogueSessionState state,
            CompiledDialogueNode node) {
        return firstPresentation(new PresentationSeed(dialogue, state.sessionId(), state.audienceId(), node), 0,
                state.revision() + 1, DialogueTransitionStatus.ADVANCED);
    }

    private DialogueTransition transition(CompiledDialogue dialogue, DialogueSessionState state,
            CompiledDialogueNode node) {
        CompiledDialogueNode.Transition transition = node.transition().orElseThrow();
        Optional<CompiledDialogueNode> target = transition.targetNodeId().flatMap(dialogue::findNode);
        if (target.isPresent()) { return firstPresentation(dialogue, state, target.orElseThrow()); }
        return transition.targetDialogueId()
                .map(targetId -> DialogueTransition.loadRequested(state, targetId))
                .orElseGet(() -> complete(state, node));
    }

    private DialogueTransition firstPresentation(
            PresentationSeed seed,
            int lineOrdinal,
            int revision,
            DialogueTransitionStatus status
    ) {
        if (!seed.node().lines().isEmpty()) {
            DialogueSessionState state = state(seed, lineOrdinal, DialoguePresentationKind.LINE, revision);
            Optional<DialogueExternalWait> wait = DialogueCommandWaits.waitFor(state, seed.node(), lineOrdinal);
            if (wait.isPresent()) {
                return new DialogueTransition(state.waiting(wait.orElseThrow()), DialogueTransitionStatus.WAITING,
                        Optional.empty(), List.of());
            }
            return new DialogueTransition(state, status, presentation(seed.dialogue(), state), List.of());
        }
        if (seed.node().hasChoices()) {
            DialogueSessionState state = state(seed, 0, DialoguePresentationKind.CHOICE, revision);
            return new DialogueTransition(state, status, presentation(seed.dialogue(), state), List.of());
        }
        DialogueSessionState state = state(seed, 0, DialoguePresentationKind.LINE, revision);
        return complete(state, seed.node());
    }

    private DialogueTransition present(CompiledDialogue dialogue, DialogueSessionState state) {
        return new DialogueTransition(state, DialogueTransitionStatus.ADVANCED, presentation(dialogue, state),
                List.of());
    }

    private DialogueSessionState state(PresentationSeed seed, int lineOrdinal, DialoguePresentationKind kind,
            int revision) {
        return new DialogueSessionState(seed.sessionId(), seed.dialogue().id(), seed.audienceId(),
                seed.node().id(), lineOrdinal, kind, DialogueSessionPhase.ACTIVE, revision, Optional.empty());
    }

    private DialogueTransition complete(DialogueSessionState state, CompiledDialogueNode node) {
        DialogueOutcomeId outcome = node.completion()
                .map(completion -> completion.outcomeId())
                .orElse(DialogueOutcomeId.DEFAULT);
        DialogueSessionState next = state.completed(outcome);
        return new DialogueTransition(next, DialogueTransitionStatus.COMPLETED, Optional.empty(), List.of());
    }

    private DialogueTransition cancel(DialogueSessionState state) {
        DialogueSessionState next = state.cancelled();
        DialogueDiagnostic diagnostic = DialogueDiagnostic.info("EDLG-CORE-002",
                DiagnosticCategory.SESSION_CANCELLED, "Dialogue session was cancelled explicitly.");
        return DialogueTransition.of(next, DialogueTransitionStatus.CANCELLED, Optional.empty(), diagnostic);
    }

    private DialogueTransition terminal(DialogueSessionState state) {
        DialogueDiagnostic diagnostic = DialogueDiagnostic.error("EDLG-CORE-003",
                DiagnosticCategory.DUPLICATE_ADVANCE_REQUEST, "Session is already closed; Advance ignored.");
        return DialogueTransition.of(state, DialogueTransitionStatus.IGNORED, Optional.empty(), diagnostic);
    }

    private DialogueTransition revisionRejected(DialogueSessionState state, int expectedRevision) {
        DiagnosticCategory category = expectedRevision < state.revision()
                ? DiagnosticCategory.DUPLICATE_ADVANCE_REQUEST
                : DiagnosticCategory.STALE_ADVANCE_REQUEST;
        DialogueDiagnostic diagnostic = DialogueDiagnostic.error("EDLG-CORE-004",
                category, "Client revision does not match the server session revision.");
        return DialogueTransition.of(state, DialogueTransitionStatus.REJECTED, Optional.empty(), diagnostic);
    }

    private DialogueTransition invalidInput(DialogueSessionState state, String message) {
        DialogueDiagnostic diagnostic = DialogueDiagnostic.error("EDLG-CORE-006",
                DiagnosticCategory.INVALID_DIALOGUE_INPUT, message);
        return DialogueTransition.of(state, DialogueTransitionStatus.REJECTED, Optional.empty(), diagnostic);
    }

    private DialogueTransition unknownChoice(DialogueSessionState state) {
        DialogueDiagnostic diagnostic = DialogueDiagnostic.error("EDLG-CORE-007",
                DiagnosticCategory.UNKNOWN_CHOICE_ID, "ChoiceId does not belong to the current node.");
        return DialogueTransition.of(state, DialogueTransitionStatus.REJECTED, Optional.empty(), diagnostic);
    }

    private DialogueTransition reject(DialogueSessionState state, String code, DiagnosticCategory category,
            String message) {
        DialogueDiagnostic diagnostic = DialogueDiagnostic.error(code, category, message);
        return DialogueTransition.of(state, DialogueTransitionStatus.REJECTED, Optional.empty(), diagnostic);
    }

    public Optional<DialoguePresentation> presentation(CompiledDialogue dialogue, DialogueSessionState state) {
        return DialoguePresentationFactory.presentation(dialogue, state);
    }

    private record PresentationSeed(CompiledDialogue dialogue, DialogueSessionId sessionId, AudienceId audienceId,
            CompiledDialogueNode node) { }
}
