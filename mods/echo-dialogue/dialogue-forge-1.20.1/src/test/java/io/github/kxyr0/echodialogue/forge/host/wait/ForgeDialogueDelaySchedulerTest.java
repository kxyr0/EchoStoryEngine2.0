package io.github.kxyr0.echodialogue.forge.host.wait;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionStatus;
import io.github.kxyr0.echodialogue.api.session.DialogueSessionView;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalResumeRequest;
import io.github.kxyr0.echodialogue.api.wait.DialogueExternalWait;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ForgeDialogueDelaySchedulerTest {
    @Test
    void resumesOnlyAfterRequestedNumberOfServerTicks() {
        ForgeDialogueDelayScheduler scheduler = new ForgeDialogueDelayScheduler();
        List<DialogueExternalResumeRequest> resumed = new ArrayList<>();

        assertTrue(scheduler.schedule(session(3), wait("delay-1", "ticks=3")));

        scheduler.tick(resumed::add);
        scheduler.tick(resumed::add);
        assertTrue(resumed.isEmpty());
        scheduler.tick(resumed::add);

        assertEquals(1, resumed.size());
        assertEquals(new DialogueSessionId("session-1"), resumed.get(0).sessionId());
        assertEquals(3, resumed.get(0).expectedRevision());
        assertEquals("delay-1", resumed.get(0).waitKey());
        assertEquals(0, scheduler.pendingCount());
    }

    @Test
    void duplicateDispatchDoesNotRestartDelay() {
        ForgeDialogueDelayScheduler scheduler = new ForgeDialogueDelayScheduler();
        List<DialogueExternalResumeRequest> resumed = new ArrayList<>();
        DialogueExternalWait wait = wait("delay-1", "ticks=2");

        assertTrue(scheduler.schedule(session(1), wait));
        scheduler.tick(resumed::add);
        assertTrue(scheduler.schedule(session(1), wait));
        scheduler.tick(resumed::add);

        assertEquals(1, resumed.size());
    }

    @Test
    void rejectsInvalidOrForeignWaits() {
        ForgeDialogueDelayScheduler scheduler = new ForgeDialogueDelayScheduler();

        assertFalse(scheduler.schedule(session(1), wait("zero", "ticks=0")));
        assertFalse(scheduler.schedule(session(1), wait("invalid", "ticks=nope")));
        assertFalse(scheduler.schedule(session(1), new DialogueExternalWait(
                "other:wait", "foreign", "ticks=1", new DialogueNodeId("greeting"), 1)));
        assertEquals(0, scheduler.pendingCount());
    }

    private DialogueSessionView session(int revision) {
        return new DialogueSessionView(new DialogueSessionId("session-1"), DialogueId.parse("sample:delay"),
                new AudienceId("audience-1"), DialogueSessionStatus.WAITING, revision, 1L, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(wait("delay-1", "ticks=3")));
    }

    private DialogueExternalWait wait(String waitKey, String payload) {
        return new DialogueExternalWait(ForgeDialogueDelayScheduler.WAIT_TYPE, waitKey, payload,
                new DialogueNodeId("greeting"), 1);
    }
}
