package io.github.kxyr0.echodialogue.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.choice.ChoiceId;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueChoice;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueCompletion;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DialogueChoiceSessionEngineTest {
    private final DialogueSessionEngine engine = new DialogueSessionEngine();
    private final DialogueSessionId sessionId = new DialogueSessionId("session-choice");
    private final AudienceId audienceId = new AudienceId("player-1");

    @Test
    void finalLineEntersOrderedChoicePresentation() {
        DialogueTransition started = engine.start(choiceDialogue(), sessionId, audienceId);

        DialogueTransition choice = engine.apply(choiceDialogue(), started.state(),
                DialogueInput.advance(sessionId, 0));

        assertEquals(DialogueTransitionStatus.ADVANCED, choice.status());
        assertEquals(DialoguePresentationKind.CHOICE, choice.presentation().orElseThrow().kind());
        assertEquals(1, choice.state().revision());
        assertEquals("Отец", choice.presentation().orElseThrow().speakerDisplayName());
        assertEquals("Ты поможешь мне?", choice.presentation().orElseThrow().text());
        assertEquals(SpeakerVisualRef.portrait("sample:father"),
                choice.presentation().orElseThrow().speakerVisual().orElseThrow());
        assertEquals("accept", choice.presentation().orElseThrow().choices().get(0).id().toString());
        assertEquals("Да, помогу.", choice.presentation().orElseThrow().choices().get(0).text());
        assertEquals("refuse", choice.presentation().orElseThrow().choices().get(1).id().toString());
    }

    @Test
    void selectedChoiceBranchesAndStoresTerminalOutcome() {
        DialogueTransition choice = choicePresentation();
        DialogueTransition acceptedLine = engine.apply(choiceDialogue(), choice.state(),
                DialogueInput.selectChoice(sessionId, 1, new ChoiceId("accept")));

        DialogueTransition completed = engine.apply(choiceDialogue(), acceptedLine.state(),
                DialogueInput.advance(sessionId, 2));

        assertEquals(DialoguePresentationKind.LINE, acceptedLine.presentation().orElseThrow().kind());
        assertEquals("Хорошо.", acceptedLine.presentation().orElseThrow().text());
        assertEquals(DialogueSessionPhase.COMPLETED, completed.state().phase());
        assertEquals("sample:accepted", completed.state().outcomeId().orElseThrow().toString());
    }

    @Test
    void invalidAndDuplicateChoiceSelectionDoesNotBranchTwice() {
        DialogueTransition choice = choicePresentation();
        DialogueTransition invalid = engine.apply(choiceDialogue(), choice.state(),
                DialogueInput.selectChoice(sessionId, 1, new ChoiceId("missing")));

        DialogueTransition acceptedLine = engine.apply(choiceDialogue(), choice.state(),
                DialogueInput.selectChoice(sessionId, 1, new ChoiceId("accept")));
        DialogueTransition duplicate = engine.apply(choiceDialogue(), acceptedLine.state(),
                DialogueInput.selectChoice(sessionId, 1, new ChoiceId("refuse")));

        assertEquals(DialogueTransitionStatus.REJECTED, invalid.status());
        assertEquals(DiagnosticCategory.UNKNOWN_CHOICE_ID, invalid.diagnostics().get(0).category());
        assertEquals("accepted", acceptedLine.state().currentNodeId().toString());
        assertEquals("accepted", duplicate.state().currentNodeId().toString());
        assertEquals(DiagnosticCategory.DUPLICATE_ADVANCE_REQUEST, duplicate.diagnostics().get(0).category());
    }

    @Test
    void legacyLinearDialogueCompletesWithDefaultOutcome() {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        CompiledDialogue dialogue = CompiledDialogue.linear(DialogueId.parse("sample:greeting"), nodeId,
                List.of(line(nodeId, 0, "Отец", "Где ты был?")));
        DialogueTransition started = engine.start(dialogue, sessionId, audienceId);

        DialogueTransition completed = engine.apply(dialogue, started.state(), DialogueInput.advance(sessionId, 0));

        assertEquals(DialogueSessionPhase.COMPLETED, completed.state().phase());
        assertEquals(DialogueOutcomeId.DEFAULT, completed.state().outcomeId().orElseThrow());
    }

    private DialogueTransition choicePresentation() {
        DialogueTransition started = engine.start(choiceDialogue(), sessionId, audienceId);
        return engine.apply(choiceDialogue(), started.state(), DialogueInput.advance(sessionId, 0));
    }

    private static CompiledDialogue choiceDialogue() {
        DialogueNodeId question = new DialogueNodeId("question");
        DialogueNodeId accepted = new DialogueNodeId("accepted");
        DialogueNodeId refused = new DialogueNodeId("refused");
        return new CompiledDialogue(DialogueId.parse("sample:question"), question, List.of(
                new CompiledDialogueNode(question, List.of(line(question, 0, "Отец", "Ты поможешь мне?")), List.of(
                        new CompiledDialogueChoice(new ChoiceId("accept"), "Да, помогу.", accepted),
                        new CompiledDialogueChoice(new ChoiceId("refuse"), "Нет.", refused)
                ), Optional.empty()),
                terminalNode(accepted, "Хорошо.", "sample:accepted"),
                terminalNode(refused, "Мне некогда.", "sample:refused")
        ));
    }

    private static CompiledDialogueNode terminalNode(DialogueNodeId id, String text, String outcome) {
        return new CompiledDialogueNode(id, List.of(line(id, 0, "Игрок", text)), List.of(),
                Optional.of(new CompiledDialogueCompletion(DialogueOutcomeId.parse(outcome))));
    }

    private static CompiledDialogueLine line(DialogueNodeId nodeId, int index, String display, String text) {
        Optional<SpeakerVisualRef> visual = display.equals("Отец")
                ? Optional.of(SpeakerVisualRef.portrait("sample:father")) : Optional.empty();
        return new CompiledDialogueLine(nodeId, index, SpeakerId.parse("sample:speaker"), display, visual, text);
    }
}
