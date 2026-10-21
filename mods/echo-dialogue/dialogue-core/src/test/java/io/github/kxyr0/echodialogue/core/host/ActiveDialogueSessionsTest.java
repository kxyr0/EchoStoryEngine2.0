package io.github.kxyr0.echodialogue.core.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueLine;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionEngine;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ActiveDialogueSessionsTest {
    @Test
    void secondActiveSessionIsRejectedWithoutOverwrite() {
        ActiveDialogueSessions sessions = new ActiveDialogueSessions(new DialogueSessionEngine());
        AudienceId audienceId = new AudienceId("player-1");

        DialogueTransition first = sessions.start(audienceId, new DialogueSessionId("session-1"), dialogue());
        DialogueTransition second = sessions.start(audienceId, new DialogueSessionId("session-2"), dialogue());

        assertEquals(DialogueTransitionStatus.STARTED, first.status());
        assertEquals(DialogueTransitionStatus.REJECTED, second.status());
        assertEquals(1, sessions.activeCount());
        assertEquals(new DialogueSessionId("session-1"), sessions.stateFor(audienceId).orElseThrow().sessionId());
        assertEquals(DiagnosticCategory.SECOND_SESSION_REJECTED, second.diagnostics().get(0).category());
    }

    @Test
    void missingAudienceDoesNotCreateImplicitSession() {
        ActiveDialogueSessions sessions = new ActiveDialogueSessions(new DialogueSessionEngine());

        assertTrue(sessions.stateFor(new AudienceId("missing")).isEmpty());
    }

    @Test
    void activeAudienceIdsExposeImmutableActiveSnapshot() {
        ActiveDialogueSessions sessions = new ActiveDialogueSessions(new DialogueSessionEngine());
        AudienceId audienceId = new AudienceId("player-1");

        sessions.start(audienceId, new DialogueSessionId("session-1"), dialogue());

        assertEquals(Set.of(audienceId), sessions.activeAudienceIds());
    }

    private static CompiledDialogue dialogue() {
        DialogueNodeId nodeId = new DialogueNodeId("greeting");
        return CompiledDialogue.linear(DialogueId.parse("sample:greeting"), nodeId, List.of(
                new CompiledDialogueLine(nodeId, 0, SpeakerId.parse("sample:father"), "Отец", "Где ты был?")
        ));
    }
}
