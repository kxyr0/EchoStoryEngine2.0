package io.github.kxyr0.echostoryengine.story.persistence.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinitionFingerprint;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionCheckpoint;
import io.github.kxyr0.echostoryengine.story.core.port.PendingActionCheckpoint;
import io.github.kxyr0.echostoryengine.story.persistence.PersistenceTestFixture;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class ExecutionRecoveryPlannerTest {
    private final ExecutionRecoveryPlanner planner = new ExecutionRecoveryPlanner();

    @Test
    void readyCheckpointRestoresWithoutAutoStart() {
        RecoveryResult result = recover(PersistenceTestFixture.readyCheckpoint(), PersistenceTestFixture.story());

        assertEquals(RecoveryStatus.RESTORED, result.status());
        assertEquals(StoryExecutionPhase.READY, result.state().phase());
        assertTrue(result.commands().isEmpty());
    }

    @Test
    void waitingCheckpointCreatesRecoveryRequestWithSameActionId() {
        RecoveryResult result = recover(PersistenceTestFixture.waitingCheckpoint(), PersistenceTestFixture.story());

        assertEquals(RecoveryStatus.RECOVERY_REQUESTED, result.status());
        assertEquals(PersistenceTestFixture.ACTION_ID, result.commands().get(0).actionExecutionId());
        assertEquals("ESE-PERSISTENCE-013", result.diagnostics().get(0).code());
    }

    @Test
    void repeatedRecoveryKeepsTheSameIdempotencyKey() {
        RecoveryResult first = recover(PersistenceTestFixture.waitingCheckpoint(), PersistenceTestFixture.story());
        RecoveryResult second = recover(PersistenceTestFixture.waitingCheckpoint(), PersistenceTestFixture.story());

        assertEquals(first.commands().get(0).actionExecutionId(), second.commands().get(0).actionExecutionId());
    }

    @Test
    void finishedCheckpointIsTerminal() {
        RecoveryResult result = recover(PersistenceTestFixture.finishedCheckpoint(), PersistenceTestFixture.story());

        assertEquals(RecoveryStatus.TERMINAL, result.status());
        assertTrue(result.commands().isEmpty());
    }

    @Test
    void failedCheckpointIsTerminal() {
        ExecutionCheckpoint checkpoint = ExecutionCheckpoint.withoutPending(
                PersistenceTestFixture.waitingCheckpoint().state().failedAt(PersistenceTestFixture.REQUEST, "x"),
                PersistenceTestFixture.fingerprint());

        RecoveryResult result = recover(checkpoint, PersistenceTestFixture.story());

        assertEquals(RecoveryStatus.TERMINAL, result.status());
    }

    @Test
    void missingDefinitionIsRejected() {
        RecoveryResult result = planner.recover(
                PersistenceTestFixture.waitingCheckpoint(),
                storyId -> Optional.empty(),
                PersistenceTestFixture.CREATED);

        assertEquals(RecoveryStatus.REJECTED, result.status());
        assertEquals("ESE-PERSISTENCE-007", result.diagnostics().get(0).code());
    }

    @Test
    void incompatibleFingerprintIsRejected() {
        ExecutionCheckpoint checkpoint = new ExecutionCheckpoint(
                PersistenceTestFixture.waitingCheckpoint().state(),
                PersistenceTestFixture.readyCheckpoint().definitionFingerprint(),
                PersistenceTestFixture.waitingCheckpoint().pendingAction());

        RecoveryResult result = recover(checkpoint, changedStory());

        assertEquals(RecoveryStatus.REJECTED, result.status());
        assertEquals("ESE-PERSISTENCE-006", result.diagnostics().get(0).code());
    }

    @Test
    void missingCurrentNodeIsRejected() {
        StoryDefinition definition = PersistenceTestFixture.storyWithoutCurrentNode();
        ExecutionCheckpoint checkpoint = new ExecutionCheckpoint(
                PersistenceTestFixture.waitingCheckpoint().state(),
                StoryDefinitionFingerprint.from(definition),
                PersistenceTestFixture.waitingCheckpoint().pendingAction());

        RecoveryResult result = recover(checkpoint, definition);

        assertEquals(RecoveryStatus.REJECTED, result.status());
        assertEquals("ESE-PERSISTENCE-008", result.diagnostics().get(0).code());
    }

    @Test
    void missingPendingActionIsRejected() {
        ExecutionCheckpoint checkpoint = ExecutionCheckpoint.withoutPending(
                PersistenceTestFixture.waitingCheckpoint().state(),
                PersistenceTestFixture.fingerprint());

        RecoveryResult result = recover(checkpoint, PersistenceTestFixture.story());

        assertEquals("ESE-PERSISTENCE-009", result.diagnostics().get(0).code());
    }

    @Test
    void mismatchedPendingTypeIsRejected() {
        PendingActionCheckpoint pending = new PendingActionCheckpoint(
                PersistenceTestFixture.ACTION_ID,
                NamespacedId.of("test:other"),
                PersistenceTestFixture.arguments());
        ExecutionCheckpoint checkpoint = new ExecutionCheckpoint(
                PersistenceTestFixture.waitingCheckpoint().state(),
                PersistenceTestFixture.fingerprint(),
                Optional.of(pending));

        RecoveryResult result = recover(checkpoint, PersistenceTestFixture.story());

        assertEquals("ESE-PERSISTENCE-009", result.diagnostics().get(0).code());
    }

    @Test
    void runningCheckpointIsRejected() {
        RecoveryResult result = recover(PersistenceTestFixture.runningCheckpoint(), PersistenceTestFixture.story());

        assertEquals(RecoveryStatus.REJECTED, result.status());
        assertEquals("ESE-PERSISTENCE-014", result.diagnostics().get(0).code());
    }

    private RecoveryResult recover(ExecutionCheckpoint checkpoint, StoryDefinition definition) {
        return planner.recover(checkpoint, storyId -> Optional.of(definition), PersistenceTestFixture.CREATED);
    }

    private StoryDefinition changedStory() {
        return new StoryDefinition(
                PersistenceTestFixture.STORY_ID,
                PersistenceTestFixture.START,
                Map.of(
                        PersistenceTestFixture.START,
                        StoryNode.start(
                                PersistenceTestFixture.START,
                                PersistenceTestFixture.REQUEST),
                        PersistenceTestFixture.REQUEST,
                        StoryNode.externalAction(
                                PersistenceTestFixture.REQUEST,
                                PersistenceTestFixture.ACTION_TYPE,
                                Map.of("target", "changed"),
                                PersistenceTestFixture.FINISH),
                        PersistenceTestFixture.FINISH,
                        StoryNode.finish(PersistenceTestFixture.FINISH)));
    }
}
