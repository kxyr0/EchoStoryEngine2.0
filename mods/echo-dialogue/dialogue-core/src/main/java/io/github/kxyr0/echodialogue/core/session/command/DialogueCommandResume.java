package io.github.kxyr0.echodialogue.core.session.command;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import io.github.kxyr0.echodialogue.core.visual.DialoguePresentationFactory;
import java.util.List;
import java.util.Optional;

public final class DialogueCommandResume {
    private DialogueCommandResume() {
    }

    public static DialogueTransition resume(
            CompiledDialogue dialogue,
            DialogueSessionState state,
            int expectedRevision,
            String waitKey
    ) {
        if (state.revision() != expectedRevision) { return revisionRejected(state, expectedRevision); }
        if (state.phase() != DialogueSessionPhase.WAITING || state.externalWait().isEmpty()) {
            return invalidInput(state, "Session is not waiting for an external integration.");
        }
        DialogueExternalWait wait = state.externalWait().orElseThrow();
        if (!wait.waitKey().equals(waitKey)) {
            return invalidInput(state, "External wait key does not match current session wait.");
        }
        CompiledDialogueNode node = dialogue.findNode(wait.continuationNodeId()).orElse(null);
        if (node == null || wait.commandPosition() > node.lines().size()) {
            return invalidInput(state, "External wait continuation is outside node lines.");
        }
        Optional<DialogueExternalWait> nextWait = DialogueCommandWaits.nextAfter(state, node, wait);
        if (nextWait.isPresent()) {
            return new DialogueTransition(state.waiting(nextWait.orElseThrow()), DialogueTransitionStatus.WAITING,
                    Optional.empty(), List.of());
        }
        if (wait.commandPosition() == node.lines().size()) {
            return terminalContinuation(dialogue, state, node);
        }
        DialogueSessionState next = new DialogueSessionState(state.sessionId(), state.dialogueId(),
                state.audienceId(), node.id(), wait.commandPosition(), DialoguePresentationKind.LINE,
                DialogueSessionPhase.ACTIVE, state.revision() + 1, Optional.empty(), Optional.empty());
        return new DialogueTransition(next, DialogueTransitionStatus.RESUMED,
                DialoguePresentationFactory.presentation(dialogue, next), List.of());
    }

    private static DialogueTransition terminalContinuation(
            CompiledDialogue dialogue,
            DialogueSessionState state,
            CompiledDialogueNode node
    ) {
        if (node.hasChoices()) {
            DialogueSessionState choice = activeState(state, node, Math.max(0, node.lines().size() - 1),
                    DialoguePresentationKind.CHOICE);
            return new DialogueTransition(choice, DialogueTransitionStatus.RESUMED,
                    DialoguePresentationFactory.presentation(dialogue, choice), List.of());
        }
        if (node.hasTransition()) {
            return transition(dialogue, state, node);
        }
        return complete(state, node);
    }

    private static DialogueTransition transition(
            CompiledDialogue dialogue,
            DialogueSessionState state,
            CompiledDialogueNode node
    ) {
        CompiledDialogueNode.Transition transition = node.transition().orElseThrow();
        Optional<CompiledDialogueNode> target = transition.targetNodeId().flatMap(dialogue::findNode);
        if (target.isPresent()) {
            return firstPresentation(dialogue, state, target.orElseThrow());
        }
        return transition.targetDialogueId()
                .map(targetId -> DialogueTransition.loadRequested(
                        activeState(state, node, Math.max(0, node.lines().size() - 1), DialoguePresentationKind.LINE),
                        targetId))
                .orElseGet(() -> complete(state, node));
    }

    private static DialogueTransition firstPresentation(
            CompiledDialogue dialogue,
            DialogueSessionState previous,
            CompiledDialogueNode node
    ) {
        if (!node.lines().isEmpty()) {
            DialogueSessionState next = activeState(previous, dialogue, node, 0, DialoguePresentationKind.LINE);
            return firstLineOrWait(dialogue, next, node);
        }
        if (node.hasChoices()) {
            DialogueSessionState next = activeState(previous, dialogue, node, 0, DialoguePresentationKind.CHOICE);
            return new DialogueTransition(next, DialogueTransitionStatus.RESUMED,
                    DialoguePresentationFactory.presentation(dialogue, next), List.of());
        }
        return transitionOrComplete(dialogue, previous, node);
    }

    private static DialogueTransition firstLineOrWait(
            CompiledDialogue dialogue,
            DialogueSessionState state,
            CompiledDialogueNode node
    ) {
        Optional<DialogueExternalWait> wait = DialogueCommandWaits.waitFor(state, node, 0);
        if (wait.isPresent()) {
            return new DialogueTransition(state.waiting(wait.orElseThrow()), DialogueTransitionStatus.WAITING,
                    Optional.empty(), List.of());
        }
        return new DialogueTransition(state, DialogueTransitionStatus.RESUMED,
                DialoguePresentationFactory.presentation(dialogue, state), List.of());
    }

    private static DialogueTransition transitionOrComplete(
            CompiledDialogue dialogue,
            DialogueSessionState state,
            CompiledDialogueNode node
    ) {
        if (node.hasTransition()) {
            return transition(dialogue, state, node);
        }
        return complete(state, node);
    }

    private static DialogueTransition complete(DialogueSessionState state, CompiledDialogueNode node) {
        DialogueOutcomeId outcome = node.completion()
                .map(completion -> completion.outcomeId())
                .orElse(DialogueOutcomeId.DEFAULT);
        DialogueSessionState completed = new DialogueSessionState(state.sessionId(), state.dialogueId(),
                state.audienceId(), node.id(), Math.max(0, node.lines().size() - 1), DialoguePresentationKind.LINE,
                DialogueSessionPhase.COMPLETED, state.revision() + 1, Optional.of(outcome), Optional.empty());
        return new DialogueTransition(completed, DialogueTransitionStatus.COMPLETED, Optional.empty(), List.of());
    }

    private static DialogueSessionState activeState(
            DialogueSessionState previous,
            CompiledDialogueNode node,
            int lineOrdinal,
            DialoguePresentationKind kind
    ) {
        return new DialogueSessionState(previous.sessionId(), previous.dialogueId(), previous.audienceId(), node.id(),
                lineOrdinal, kind, DialogueSessionPhase.ACTIVE, previous.revision() + 1, Optional.empty(),
                Optional.empty());
    }

    private static DialogueSessionState activeState(
            DialogueSessionState previous,
            CompiledDialogue dialogue,
            CompiledDialogueNode node,
            int lineOrdinal,
            DialoguePresentationKind kind
    ) {
        return new DialogueSessionState(previous.sessionId(), dialogue.id(), previous.audienceId(), node.id(),
                lineOrdinal, kind, DialogueSessionPhase.ACTIVE, previous.revision() + 1, Optional.empty(),
                Optional.empty());
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
