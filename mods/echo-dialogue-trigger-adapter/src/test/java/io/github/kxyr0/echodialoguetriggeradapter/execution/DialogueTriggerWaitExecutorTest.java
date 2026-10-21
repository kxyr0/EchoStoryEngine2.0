package io.github.kxyr0.echodialoguetriggeradapter.execution;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialoguetriggeradapter.bridge.DialogueTriggerSessionKey;
import io.github.kxyr0.echodialoguetriggeradapter.bridge.DialogueTriggerWaitRegistration;
import io.github.kxyr0.echodialoguetriggeradapter.bridge.DialogueTriggerWaitRequest;
import io.github.kxyr0.echotrigger.api.id.TriggerCommandId;
import io.github.kxyr0.echotrigger.api.id.TriggerExecutionId;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.api.id.TriggerInstanceId;
import io.github.kxyr0.echotrigger.api.id.TriggerOwnerId;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echotrigger.api.model.TriggerResult;
import io.github.kxyr0.echotrigger.api.model.TriggerSnapshot;
import io.github.kxyr0.echotrigger.api.status.TriggerOutcome;
import io.github.kxyr0.echotrigger.api.status.TriggerResultCode;
import io.github.kxyr0.echotrigger.api.status.TriggerStatus;
import io.github.kxyr0.echotrigger.forge.api.TriggerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueTriggerWaitExecutorTest {
    private final DialogueTriggerSessionKey key = new DialogueTriggerSessionKey(
            new DialogueSessionId("session-1"), "WaitForExit", 4, "jdh:leave_house");
    private final TriggerOwnerId owner = new TriggerOwnerId("player:a");
    private final TriggerId triggerId = TriggerId.parse("jdh:leave_house");

    @Test
    void resourceWaitStartsTriggerThroughPublicService() {
        RecordingService service = new RecordingService();
        DialogueTriggerWaitRequest request = new DialogueTriggerWaitRequest(
                key, owner, Optional.of(triggerId), Optional.empty());

        DialogueTriggerWaitRegistration registration = new DialogueTriggerWaitExecutor().wait(service, request);

        assertFalse(registration.inline());
        assertEquals(1, service.started);
        assertEquals("resource-instance", registration.instanceId().value());
    }

    @Test
    void cleanupStopsOnlyInlineRegistrations() {
        RecordingService service = new RecordingService();
        DialogueTriggerWaitRegistration inline = new DialogueTriggerWaitRegistration(
                key, new TriggerInstanceId("inline-a"), true);
        DialogueTriggerWaitRegistration resource = new DialogueTriggerWaitRegistration(
                key, new TriggerInstanceId("resource-a"), false);

        var results = new DialogueTriggerCleanup().cleanupInline(service, List.of(inline, resource));

        assertEquals(1, service.stopped);
        assertEquals(1, results.size());
        assertTrue(results.get(0).snapshot().orElseThrow().status().isTerminal());
    }

    private final class RecordingService implements TriggerService {
        private int started;
        private int stopped;

        @Override
        public TriggerResult start(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId) {
            started++;
            return TriggerResult.of(TriggerResultCode.STARTED,
                    snapshot(id, new TriggerInstanceId("resource-instance"), TriggerStatus.WAITING));
        }

        @Override
        public TriggerResult stop(TriggerInstanceId instanceId, TriggerCommandId commandId) {
            stopped++;
            return TriggerResult.of(TriggerResultCode.STOPPED,
                    snapshot(triggerId, instanceId, TriggerStatus.STOPPED));
        }

        @Override
        public TriggerResult reset(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId) {
            return TriggerResult.rejected(TriggerResultCode.NOT_FOUND, "not used");
        }

        @Override
        public TriggerResult fire(TriggerId id, TriggerOwnerId owner, TriggerCommandId commandId) {
            return TriggerResult.rejected(TriggerResultCode.NOT_FOUND, "not used");
        }

        private TriggerSnapshot snapshot(TriggerId id, TriggerInstanceId instanceId, TriggerStatus status) {
            return new TriggerSnapshot(id, instanceId, new TriggerExecutionId("exec"),
                    status, TriggerOutcome.NONE, 0, owner, new TriggerRevision(1));
        }
    }
}
