package io.github.kxyr0.echodialogue.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentation;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DialogueSessionEngineTest {
    private final DialogueSessionEngine engine = new DialogueSessionEngine();
    private final CompiledDialogue dialogue = sampleDialogue();
    private final DialogueSessionId sessionId = new DialogueSessionId("session-1");

    @Test
    void startExposesFirstLine() {
        DialogueTransition transition = engine.start(dialogue, sessionId, new AudienceId("player-1"));

        assertEquals(DialogueTransitionStatus.STARTED, transition.status());
        assertEquals(DialogueSessionPhase.ACTIVE, transition.state().phase());
        assertEquals(0, transition.state().revision());
        assertEquals("Отец", transition.presentation().orElseThrow().speakerDisplayName());
        assertEquals("Где ты был?", transition.presentation().orElseThrow().text());
        assertEquals(new DialoguePresentation.Cue("faceAnimation", "sample:look_left"),
                transition.presentation().orElseThrow().cues().get(0));
        assertEquals(SpeakerVisualRef.skinHead("sample:father"),
                transition.presentation().orElseThrow().speakerVisual().orElseThrow());
    }

    @Test
    void advanceMovesExactlyOneLineAndIncrementsRevision() {
        DialogueTransition started = engine.start(dialogue, sessionId, new AudienceId("player-1"));

        DialogueTransition advanced = engine.apply(dialogue, started.state(), DialogueInput.advance(sessionId, 0));

        assertEquals(DialogueTransitionStatus.ADVANCED, advanced.status());
        assertEquals(1, advanced.state().lineIndex());
        assertEquals(1, advanced.state().revision());
        assertEquals("#57C7FF", advanced.presentation().orElseThrow().speakerNameColor().orElseThrow().hex());
        assertEquals("Гулял.", advanced.presentation().orElseThrow().text());
    }

    @Test
    void npcCommandPausesDialogueUntilExternalResume() {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        CompiledDialogue commandDialogue = new CompiledDialogue(DialogueId.parse("sample:move"), nodeId,
                List.of(new CompiledDialogueNode(nodeId, List.of(
                        fatherLine(nodeId, 0, "РРґСѓ Рє РґРІРµСЂРё.", List.of()),
                        line(nodeId, 1, "sample:father", "РћС‚РµС†", "РЇ РЅР° РјРµСЃС‚Рµ.")),
                        List.of(), List.of(new CompiledDialogueCommand("echo_navigation:npc_move", 1,
                        Map.of("npc", "sample:father", "x", "200", "y", "64", "z", "180"))),
                        Optional.empty(), Optional.empty())));
        DialogueTransition started = engine.start(commandDialogue, sessionId, new AudienceId("player-1"));

        DialogueTransition waiting = engine.apply(
                commandDialogue, started.state(), DialogueInput.advance(sessionId, 0));
        DialogueTransition resumed = engine.resumeExternal(commandDialogue, waiting.state(), 1,
                waiting.state().externalWait().orElseThrow().waitKey());

        assertEquals(DialogueTransitionStatus.WAITING, waiting.status());
        assertEquals("echo_navigation:npc_move", waiting.state().externalWait().orElseThrow().waitType());
        assertEquals(DialogueTransitionStatus.RESUMED, resumed.status());
        assertEquals("РЇ РЅР° РјРµСЃС‚Рµ.", resumed.presentation().orElseThrow().text());
    }

    @Test
    void finalAdvanceCompletesAndRepeatedAdvanceIsIgnored() {
        DialogueTransition first = engine.start(dialogue, sessionId, new AudienceId("player-1"));
        DialogueTransition second = engine.apply(dialogue, first.state(), DialogueInput.advance(sessionId, 0));
        DialogueTransition third = engine.apply(dialogue, second.state(), DialogueInput.advance(sessionId, 1));

        DialogueTransition completed = engine.apply(dialogue, third.state(), DialogueInput.advance(sessionId, 2));
        DialogueTransition ignored = engine.apply(dialogue, completed.state(), DialogueInput.advance(sessionId, 3));

        assertEquals(DialogueSessionPhase.COMPLETED, completed.state().phase());
        assertFalse(completed.presentation().isPresent());
        assertEquals(DialogueTransitionStatus.IGNORED, ignored.status());
        assertEquals(DiagnosticCategory.DUPLICATE_ADVANCE_REQUEST, ignored.diagnostics().get(0).category());
    }

    @Test
    void wrongSessionIdDoesNotAdvanceState() {
        DialogueTransition started = engine.start(dialogue, sessionId, new AudienceId("player-1"));

        DialogueTransition rejected = engine.apply(
                dialogue, started.state(), DialogueInput.advance(new DialogueSessionId("other"), 0));

        assertEquals(DialogueTransitionStatus.REJECTED, rejected.status());
        assertEquals(0, rejected.state().lineIndex());
        assertEquals(DiagnosticCategory.WRONG_PLAYER_REQUEST, rejected.diagnostics().get(0).category());
    }

    @Test
    void staleRevisionCannotAdvanceTwice() {
        DialogueTransition started = engine.start(dialogue, sessionId, new AudienceId("player-1"));
        DialogueTransition advanced = engine.apply(dialogue, started.state(), DialogueInput.advance(sessionId, 0));

        DialogueTransition duplicate = engine.apply(dialogue, advanced.state(), DialogueInput.advance(sessionId, 0));

        assertEquals(DialogueTransitionStatus.REJECTED, duplicate.status());
        assertEquals(1, duplicate.state().lineIndex());
        assertEquals(DiagnosticCategory.DUPLICATE_ADVANCE_REQUEST, duplicate.diagnostics().get(0).category());
    }

    @Test
    void cancellationClosesSessionExplicitly() {
        DialogueTransition started = engine.start(dialogue, sessionId, new AudienceId("player-1"));

        DialogueTransition cancelled = engine.apply(dialogue, started.state(), DialogueInput.cancel(sessionId, 0));

        assertEquals(DialogueTransitionStatus.CANCELLED, cancelled.status());
        assertEquals(DialogueSessionPhase.CANCELLED, cancelled.state().phase());
        assertTrue(cancelled.presentation().isEmpty());
    }

    @Test
    void externalWaitPausesAndResumesAtContinuationLine() {
        DialogueTransition started = engine.start(dialogue, sessionId, new AudienceId("player-1"));
        DialogueExternalWait wait = new DialogueExternalWait(
                "echo_trigger", "trigger-instance-1", "PlayerEnter(Exit)",
                new DialogueNodeId("greeting"), 2);

        DialogueTransition waiting = engine.waitExternal(dialogue, started.state(), 0, wait);
        DialogueTransition rejected = engine.apply(dialogue, waiting.state(), DialogueInput.advance(sessionId, 1));
        DialogueTransition resumed = engine.resumeExternal(dialogue, waiting.state(), 1, "trigger-instance-1");

        assertEquals(DialogueTransitionStatus.WAITING, waiting.status());
        assertEquals(DialogueSessionPhase.WAITING, waiting.state().phase());
        assertEquals(wait, waiting.state().externalWait().orElseThrow());
        assertEquals(DialogueTransitionStatus.REJECTED, rejected.status());
        assertEquals(DialogueTransitionStatus.RESUMED, resumed.status());
        assertEquals(DialogueSessionPhase.ACTIVE, resumed.state().phase());
        assertEquals(dialogue.startNode().lines().get(2).text(), resumed.presentation().orElseThrow().text());
    }

    @Test
    void dialogueLoadRequestsCatalogTargetAndStartsLoadedDialogueOnSameSession() {
        DialogueId targetId = DialogueId.parse("sample:street");
        CompiledDialogue source = loadingDialogue(targetId);
        CompiledDialogue target = loadedDialogue(targetId);
        DialogueTransition started = engine.start(source, sessionId, new AudienceId("player-1"));

        DialogueTransition requested = engine.apply(source, started.state(), DialogueInput.advance(sessionId, 0));
        DialogueTransition loaded = engine.load(target, requested.state());

        assertEquals(DialogueTransitionStatus.LOAD_REQUESTED, requested.status());
        assertEquals(targetId, requested.targetDialogueId().orElseThrow());
        assertEquals(DialogueTransitionStatus.LOADED, loaded.status());
        assertEquals(targetId, loaded.state().dialogueId());
        assertEquals(sessionId, loaded.state().sessionId());
        assertEquals(1, loaded.state().revision());
        assertEquals("Улица.", loaded.presentation().orElseThrow().text());
    }

    private static CompiledDialogue sampleDialogue() {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return CompiledDialogue.linear(DialogueId.parse("sample:greeting"), nodeId, List.of(
                fatherLine(nodeId, 0, "Где ты был?",
                        List.of(new CompiledDialogueLine.Cue("faceAnimation", "sample:look_left"))),
                line(nodeId, 1, "sample:player", "Игрок", "Гулял."),
                line(nodeId, 2, "sample:father", "Отец", "Понятно.")
        ));
    }

    private static CompiledDialogue loadingDialogue(DialogueId targetId) {
        DialogueNodeId nodeId = new DialogueNodeId("home");
        CompiledDialogueNode node = new CompiledDialogueNode(nodeId,
                List.of(line(nodeId, 0, "sample:father", "Отец", "Иди.")), List.of(),
                Optional.empty(), Optional.of(CompiledDialogueNode.Transition.load(targetId)));
        return new CompiledDialogue(DialogueId.parse("sample:home"), nodeId, List.of(node));
    }

    private static CompiledDialogue loadedDialogue(DialogueId targetId) {
        DialogueNodeId nodeId = new DialogueNodeId("street");
        return CompiledDialogue.linear(targetId, nodeId, List.of(
                line(nodeId, 0, "sample:player", "Игрок", "Улица.")));
    }

    private static CompiledDialogueLine line(
            DialogueNodeId nodeId,
            int index,
            String speakerId,
            String display,
            String text
    ) {
        Optional<SpeakerVisualRef> visual = speakerId.equals("sample:father")
                ? Optional.of(SpeakerVisualRef.skinHead("sample:father")) : Optional.empty();
        Optional<SpeakerNameColor> color = speakerId.equals("sample:player")
                ? Optional.of(SpeakerNameColor.parse("#57C7FF")) : Optional.empty();
        return new CompiledDialogueLine(nodeId, index, SpeakerId.parse(speakerId), display, color, visual, text,
                Optional.empty(), false, List.of());
    }

    private static CompiledDialogueLine fatherLine(
            DialogueNodeId nodeId,
            int index,
            String text,
            List<CompiledDialogueLine.Cue> cues
    ) {
        return new CompiledDialogueLine(nodeId, index, SpeakerId.parse("sample:father"), "Отец",
                Optional.of(SpeakerVisualRef.skinHead("sample:father")), text, Optional.empty(), false, cues);
    }
}
