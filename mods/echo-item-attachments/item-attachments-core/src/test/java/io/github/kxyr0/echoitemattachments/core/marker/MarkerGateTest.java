package io.github.kxyr0.echoitemattachments.core.marker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoitemattachments.api.command.MarkerSignal;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import io.github.kxyr0.echoitemattachments.api.marker.ItemAttachmentMarkers;
import org.junit.jupiter.api.Test;

class MarkerGateTest {
    @Test
    void requiresSubjectPlaybackRevisionPhaseAndMarker() {
        MarkerGate gate = new MarkerGate();
        AttachmentCommandId command = new AttachmentCommandId("test:command");
        AttachmentSubjectRef subject = new AttachmentSubjectRef(SubjectKind.PLAYER,
                "f3edc5ea-47e7-44f9-ada4-195c195d5ae8");
        gate.bind(new MarkerBinding(command, subject, "test:take", "test:playback",
                8L, "MAIN", ItemAttachmentMarkers.ATTACH_RIGHT_HAND));
        MarkerSignal wrong = new MarkerSignal(subject, "test:take", "test:playback",
                7L, "MAIN", ItemAttachmentMarkers.ATTACH_RIGHT_HAND, 20L);
        assertEquals(MarkerDecision.Status.STALE, gate.accept(command, wrong).status());
        MarkerSignal exact = new MarkerSignal(subject, "test:take", "test:playback",
                8L, "MAIN", ItemAttachmentMarkers.ATTACH_RIGHT_HAND, 21L);
        assertEquals(MarkerDecision.Status.ACCEPTED, gate.accept(command, exact).status());
    }
}
