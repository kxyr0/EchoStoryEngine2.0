package io.github.kxyr0.echodialogue.persistence;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprinter;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.session.DialogueInput;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import java.util.List;

public final class PersistenceTestFixtures {
    public static final DialogueSessionId SESSION_ID = new DialogueSessionId("session-1");
    public static final DialogueSessionId TERMINAL_SESSION_ID = new DialogueSessionId("terminal-session");
    static final AudienceId AUDIENCE_ID = new AudienceId("audience-1");

    private PersistenceTestFixtures() {
    }

    public static CompiledDialogue dialogue() {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return CompiledDialogue.linear(DialogueId.parse("sample:greeting"), nodeId, List.of(
                new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"), "Отец", "Где ты был?"),
                new CompiledDialogueLine(nodeId, 1, SpeakerId.parse("sample:player"), "Игрок", "Гулял.")
        ));
    }

    public static CompiledDialogue changedDialogue() {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return CompiledDialogue.linear(DialogueId.parse("sample:greeting"), nodeId, List.of(
                new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"), "Отец", "Где ты был?"),
                new CompiledDialogueLine(nodeId, 1, SpeakerId.parse("sample:player"), "Игрок", "Молчал.")
        ));
    }

    public static DialogueCheckpoint activeCheckpoint(long now) {
        CompiledDialogue dialogue = dialogue();
        DialogueSessionState state = new DialogueSessionEngine()
                .start(dialogue, SESSION_ID, AUDIENCE_ID)
                .state();
        return DialogueCheckpoint.active(state, dialogue, CompiledDialogueFingerprinter.fingerprint(dialogue), now);
    }

    public static DialogueCheckpoint completedCheckpoint(long now) {
        CompiledDialogue dialogue = dialogue();
        DialogueSessionEngine engine = new DialogueSessionEngine();
        DialogueSessionState first = engine.start(dialogue, TERMINAL_SESSION_ID, AUDIENCE_ID).state();
        DialogueSessionState second = engine.apply(dialogue, first, DialogueInput.advance(TERMINAL_SESSION_ID, 0))
                .state();
        DialogueSessionState completed = engine.apply(dialogue, second, DialogueInput.advance(TERMINAL_SESSION_ID, 1))
                .state();
        return DialogueCheckpoint.terminal(completed, dialogue, CompiledDialogueFingerprinter.fingerprint(dialogue),
                now, DialogueCheckpoint.TerminalFields.completed(completed.outcomeId().orElseThrow()));
    }
}
