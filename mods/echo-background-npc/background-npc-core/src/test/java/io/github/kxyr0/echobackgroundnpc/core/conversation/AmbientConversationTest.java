package io.github.kxyr0.echobackgroundnpc.core.conversation;

import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientDialogue;
import io.github.kxyr0.echobackgroundnpc.api.conversation.AmbientLine;
import io.github.kxyr0.echobackgroundnpc.api.conversation.ConversationOutcome;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.AmbientDialogueId;
import io.github.kxyr0.echobackgroundnpc.api.source.BackgroundSourceRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AmbientConversationTest {
    @Test
    void linesAdvanceAutomaticallyAndSessionPinsRevision() {
        AmbientReadingTime timing = new AmbientReadingTime(1000, 20, 100);
        AmbientConversationSession session = new AmbientConversationSession("day2/590",
                id("mira"), id("anton"), dialogue(4L), 0L, timing);
        assertEquals("Первая", session.current().text());
        assertTrue(session.advance(1000, timing).isPresent());
        assertEquals("Вторая", session.current().text());
        session.advance(2000, timing);
        assertEquals(Optional.of(ConversationOutcome.COMPLETED), session.outcome());
        assertEquals(4L, session.dialogue().revision());
    }

    @Test
    void storyOverlayAndVoiceAlwaysWin() {
        AmbientOverlaySelector overlays = new AmbientOverlaySelector();
        var candidates = List.of(new AmbientOverlaySelector.Candidate("b", 4, true),
                new AmbientOverlaySelector.Candidate("a", 4, true));
        assertEquals("a", overlays.select(candidates, 12, false).orElseThrow().sessionId());
        assertTrue(overlays.select(candidates, 12, true).isEmpty());
        AmbientVoiceBudget voices = new AmbientVoiceBudget(2, 12);
        var voiceCandidates = List.of(new AmbientVoiceBudget.VoiceCandidate("a", 4),
                new AmbientVoiceBudget.VoiceCandidate("b", 9),
                new AmbientVoiceBudget.VoiceCandidate("far", 10_000));
        assertEquals(2, voices.eligible(voiceCandidates, false).size());
        assertTrue(voices.eligible(voiceCandidates, true).isEmpty());
    }

    @Test
    void leavingRangeSuppressesPresentationWithoutRestartingSession() {
        AmbientConversationSession session = new AmbientConversationSession("talk", id("mira"), id("anton"),
                dialogue(1), 0, AmbientReadingTime.defaults());
        AmbientOverlaySelector selector = new AmbientOverlaySelector();
        assertFalse(selector.select(List.of(new AmbientOverlaySelector.Candidate("talk", 400, true)),
                12, false).isPresent());
        assertEquals(0, session.lineIndex());
        assertTrue(session.active());
    }

    private AmbientDialogue dialogue(long revision) {
        var source = new BackgroundSourceRef("talk.es", 1, 1);
        return new AmbientDialogue(new AmbientDialogueId("jdh:talk"), List.of(
                new AmbientLine("mira", "Первая", Optional.empty(), source),
                new AmbientLine("anton", "Вторая", Optional.empty(), source)), revision);
    }
    private BackgroundNpcId id(String value) { return new BackgroundNpcId("jdh:" + value); }
}
