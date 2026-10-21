package io.github.kxyr0.echodialogue.core.visual;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionState;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DialoguePresentationFactoryTest {
    @Test
    void presentationCarriesThoughtFlagFromCompiledLine() {
        DialogueNodeId nodeId = new DialogueNodeId("thought");
        CompiledDialogue dialogue = CompiledDialogue.linear(DialogueId.parse("sample:thought"), nodeId,
                List.of(new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"), "Father",
                        Optional.of(SpeakerVisualRef.skinHead("sample:father")), "Stay quiet.",
                        Optional.empty(), true, List.of())));
        DialogueSessionState state = new DialogueSessionState(
                new DialogueSessionId("session-1"), dialogue.id(), new AudienceId("player-1"), nodeId, 0,
                DialoguePresentationKind.LINE, DialogueSessionPhase.ACTIVE, 0, Optional.empty());

        assertTrue(DialoguePresentationFactory.presentation(dialogue, state).orElseThrow().thought());
    }
}
