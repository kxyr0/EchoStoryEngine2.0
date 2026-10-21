package io.github.kxyr0.echodialogue.core.visual;

import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import java.util.List;
import java.util.Optional;

public final class DialoguePresentationFactory {
    private DialoguePresentationFactory() {
    }

    public static Optional<DialoguePresentation> presentation(CompiledDialogue dialogue, DialogueSessionState state) {
        if (state.phase() != DialogueSessionPhase.ACTIVE) { return Optional.empty(); }
        DialoguePresentation.Context context = new DialoguePresentation.Context(
                state.sessionId(), state.dialogueId(), state.audienceId());
        CompiledDialogueNode node = dialogue.findNode(state.currentNodeId()).orElseThrow();
        if (state.presentationKind() == DialoguePresentationKind.CHOICE) {
            return choicePresentation(context, state, node);
        }
        CompiledDialogueLine line = node.lines().get(state.lineOrdinal());
        return Optional.of(DialoguePresentation.line(context, state.revision(), prompt(line), true));
    }

    private static Optional<DialoguePresentation> choicePresentation(
            DialoguePresentation.Context context,
            DialogueSessionState state,
            CompiledDialogueNode node
    ) {
        List<DialoguePresentation.VisibleChoice> choices = node.choices().stream()
                .map(choice -> new DialoguePresentation.VisibleChoice(choice.id(), choice.text()))
                .toList();
        if (node.lines().isEmpty()) {
            return Optional.of(DialoguePresentation.choice(context, state.revision(), choices));
        }
        CompiledDialogueLine line = node.lines().get(Math.min(state.lineOrdinal(), node.lines().size() - 1));
        return Optional.of(DialoguePresentation.choice(context, state.revision(), prompt(line), choices));
    }

    private static DialoguePresentation.Prompt prompt(CompiledDialogueLine line) {
        return new DialoguePresentation.Prompt(
                line.speakerId(), line.speakerDisplayName(), line.speakerNameColor(), line.speakerVisual(),
                line.text(),
                line.thought(),
                line.cues().stream()
                        .map(cue -> new DialoguePresentation.Cue(cue.name(), cue.value()))
                        .toList());
    }
}
