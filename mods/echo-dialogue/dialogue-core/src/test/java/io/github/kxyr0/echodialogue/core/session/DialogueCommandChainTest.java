package io.github.kxyr0.echodialogue.core.session;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueCompletion;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class DialogueCommandChainTest {
    private static final DialogueSessionId SESSION_ID = new DialogueSessionId("session-1");
    private final DialogueSessionEngine engine = new DialogueSessionEngine();

    @Test
    void multipleCommandsAtSamePositionPauseInOrderBeforeLine() {
        CompiledDialogue dialogue = dialogue();
        DialogueTransition started = engine.start(dialogue, SESSION_ID, new AudienceId("player-1"));

        DialogueTransition first = engine.apply(dialogue, started.state(), DialogueInput.advance(SESSION_ID, 0));
        DialogueTransition second = engine.resumeExternal(dialogue, first.state(), 1,
                first.state().externalWait().orElseThrow().waitKey());
        DialogueTransition resumed = engine.resumeExternal(dialogue, second.state(), 2,
                second.state().externalWait().orElseThrow().waitKey());

        assertEquals(DialogueTransitionStatus.WAITING, first.status());
        assertEquals("test:first", first.state().externalWait().orElseThrow().waitType());
        assertEquals(DialogueTransitionStatus.WAITING, second.status());
        assertEquals("test:second", second.state().externalWait().orElseThrow().waitType());
        assertEquals(DialogueTransitionStatus.RESUMED, resumed.status());
        assertEquals("after commands", resumed.presentation().orElseThrow().text());
    }

    @Test
    void commandAfterLastLineCompletesWithoutPresentingAnotherLine() {
        CompiledDialogue dialogue = terminalCommandDialogue();
        DialogueTransition started = engine.start(dialogue, SESSION_ID, new AudienceId("player-1"));

        DialogueTransition waiting = engine.apply(dialogue, started.state(), DialogueInput.advance(SESSION_ID, 0));
        DialogueTransition completed = engine.resumeExternal(dialogue, waiting.state(), 1,
                waiting.state().externalWait().orElseThrow().waitKey());

        assertEquals(DialogueTransitionStatus.WAITING, waiting.status());
        assertEquals("test:stop", waiting.state().externalWait().orElseThrow().waitType());
        assertEquals(DialogueTransitionStatus.COMPLETED, completed.status());
        assertEquals(Optional.empty(), completed.presentation());
        assertEquals(DialogueOutcomeId.parse("sample:done"), completed.state().outcomeId().orElseThrow());
    }

    private static CompiledDialogue dialogue() {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return new CompiledDialogue(DialogueId.parse("sample:chain"), nodeId, List.of(
                new CompiledDialogueNode(nodeId, List.of(line(nodeId, 0, "start"),
                        line(nodeId, 1, "after commands")), List.of(), commands(), Optional.empty(),
                        Optional.empty())));
    }

    private static CompiledDialogue terminalCommandDialogue() {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return new CompiledDialogue(DialogueId.parse("sample:terminal_command"), nodeId, List.of(
                new CompiledDialogueNode(nodeId, List.of(line(nodeId, 0, "last line")), List.of(),
                        List.of(new CompiledDialogueCommand("test:stop", 1, Map.of("order", "last"))),
                        Optional.of(new CompiledDialogueCompletion(DialogueOutcomeId.parse("sample:done"))),
                        Optional.empty())));
    }

    private static List<CompiledDialogueCommand> commands() {
        return List.of(
                new CompiledDialogueCommand("test:first", 1, Map.of("order", "1")),
                new CompiledDialogueCommand("test:second", 1, Map.of("order", "2")));
    }

    private static CompiledDialogueLine line(DialogueNodeId nodeId, int index, String text) {
        return new CompiledDialogueLine(nodeId, index, SpeakerId.parse("sample:father"),
                "Father", Optional.empty(), text, Optional.empty(), false, List.of());
    }
}
