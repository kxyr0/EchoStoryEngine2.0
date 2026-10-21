package io.github.kxyr0.echodialogue.forge.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprint;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryRevision;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echoevents.api.catalog.EventKind;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class DialogueDomainEventsTest {
    @Test void hydrationWithoutOutboxDoesNotManufactureCompletion() {
        assertTrue(DialogueDomainEvents.occurrences(checkpoint(Map.of()), true).isEmpty());
    }
    @Test void committedCompletionOutboxHasStableIdentityAndCausalContextAcrossRestart() {
        var checkpoint = checkpoint(Map.of("domainEvent.phase", "COMPLETED", "domainEvent.tick", "194",
                "domainEvent.root", "SceneExecution#51", "domainEvent.caller", "SceneExecution#51"));
        var live = DialogueDomainEvents.occurrences(checkpoint, false).get(0);
        var recovered = DialogueDomainEvents.occurrences(checkpoint, true).get(0);
        assertEquals(live, recovered);
        assertEquals(EventKind.DIALOGUE_COMPLETED, live.kind());
        assertEquals("SceneExecution#51", live.source().rootExecution().value());
        assertEquals("SceneExecution#51", live.source().callerExecution().orElseThrow().value());
        assertEquals(194, live.source().gameTick());
    }
    @Test void choiceIsIndependentOfTerminalCompletionAndStartedIsNotReplayed() {
        var events = DialogueDomainEvents.occurrences(checkpoint(Map.of("domainEvent.phase", "COMPLETED",
                "domainEvent.choice", "tell_truth")), true);
        assertEquals(2, events.size());
        assertNotEquals(events.get(0).id(), events.get(1).id());
        assertEquals(EventKind.DIALOGUE_CHOICE, events.get(1).kind());
        assertTrue(DialogueDomainEvents.occurrences(checkpoint(Map.of("domainEvent.phase", "STARTED")),
                true).isEmpty());
        assertEquals(EventKind.DIALOGUE_CANCELLED, DialogueDomainEvents.occurrences(checkpoint(
                Map.of("domainEvent.phase", "CANCELLED")), true).get(0).kind());
    }
    @Test void initialTerminalStillStartsAndFailureStaysDistinct() {
        var initial = checkpoint(Map.of("domainEvent.started", "true", "domainEvent.phase", "COMPLETED"));
        assertEquals(java.util.List.of(EventKind.DIALOGUE_STARTED, EventKind.DIALOGUE_COMPLETED),
                DialogueDomainEvents.occurrences(initial, false).stream().map(value -> value.kind()).toList());
        assertEquals(1, DialogueDomainEvents.occurrences(initial, true).size());
        var failed = checkpoint(Map.of("domainEvent.phase", "FAILED", "domainEvent.failure", "PROVIDER_LOST"));
        assertEquals(EventKind.DIALOGUE_FAILED, DialogueDomainEvents.occurrences(failed, true).get(0).kind());
    }
    private DialogueCheckpoint checkpoint(Map<String, String> metadata) {
        return new DialogueCheckpoint(new DialogueSessionId("dialogue-194"), DialogueId.parse("jdh:father_dialogue"),
                new CompiledDialogueFingerprint("a".repeat(64)), new AudienceId("00000000-0000-0000-0000-000000000001"),
                DialogueSessionPhase.COMPLETED, 5, new DialogueNodeId("end"), DialoguePresentationKind.LINE, 0,
                0, 1, Optional.of("default"), Optional.empty(), metadata, new DialogueRepositoryRevision(5));
    }
}
