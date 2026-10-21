package io.github.kxyr0.echostoryengine.story.persistence.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.core.application.StoryRuntimeUseCase;
import io.github.kxyr0.echostoryengine.story.core.domain.RuntimeTransitionPolicy;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadStatus;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.persistence.PersistenceTestFixture;
import io.github.kxyr0.echostoryengine.story.persistence.file.FileExecutionStateRepository;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CrashWindowRecoveryTest {
    @TempDir
    private Path root;

    @Test
    void crashAfterWaitingSaveBeforeDispatchRecoversSameCommand() {
        StoryRuntimeResult started = runtime(repository()).advance(startInput());

        RecoveryResult recovered = recoverFromFreshRepository();

        assertEquals(PersistenceTestFixture.ACTION_ID, started.commands().get(0).actionExecutionId());
        assertEquals(PersistenceTestFixture.ACTION_ID, recovered.commands().get(0).actionExecutionId());
    }

    @Test
    void crashAfterDispatchBeforeCompletionSaveRecoversSameCommandAgain() {
        runtime(repository()).advance(startInput());

        RecoveryResult recovered = recoverFromFreshRepository();

        assertEquals(RecoveryStatus.RECOVERY_REQUESTED, recovered.status());
        assertEquals(PersistenceTestFixture.ACTION_ID, recovered.commands().get(0).actionExecutionId());
    }

    @Test
    void recoveredCompletionFinishesAndSavesFinalState() {
        runtime(repository()).advance(startInput());
        recoverFromFreshRepository();
        StoryRuntimeResult finished = runtime(repository()).advance(completionInput());

        assertEquals(StoryExecutionPhase.FINISHED, finished.state().phase());
        assertTrue(repository().load(PersistenceTestFixture.EXECUTION_ID)
                .checkpoint()
                .orElseThrow()
                .pendingAction()
                .isEmpty());
    }

    @Test
    void duplicateCompletionAfterRecoveryDoesNotAdvanceAgain() {
        StoryRuntimeUseCase runtime = runtime(repository());
        runtime.advance(startInput());
        runtime.advance(completionInput());

        StoryRuntimeResult duplicate = runtime.advance(completionInput());

        assertEquals(0, duplicate.commands().size());
        assertEquals("ESE-ACTION-003", duplicate.diagnostics().get(0).code());
    }

    private RecoveryResult recoverFromFreshRepository() {
        ExecutionLoadResult loaded = repository().load(PersistenceTestFixture.EXECUTION_ID);
        assertEquals(ExecutionLoadStatus.FOUND, loaded.status());
        return new ExecutionRecoveryPlanner().recover(
                loaded.checkpoint().orElseThrow(),
                storyId -> Optional.of(PersistenceTestFixture.story()),
                PersistenceTestFixture.CREATED);
    }

    private StoryRuntimeUseCase runtime(FileExecutionStateRepository repository) {
        RuntimeTransitionPolicy policy = new RuntimeTransitionPolicy(
                PersistenceTestFixture.story(),
                Set.of(PersistenceTestFixture.ACTION_TYPE),
                16);
        StoryClock clock = new PersistenceTestFixture.MutableClock();
        return new StoryRuntimeUseCase(repository, diagnostic -> { }, clock, policy);
    }

    private FileExecutionStateRepository repository() {
        return new FileExecutionStateRepository(root, new PersistenceTestFixture.MutableClock());
    }

    private StoryRuntimeInput startInput() {
        return StoryRuntimeInput.start(PersistenceTestFixture.STORY_ID, PersistenceTestFixture.EXECUTION_ID);
    }

    private StoryRuntimeInput completionInput() {
        return StoryRuntimeInput.externalActionCompleted(
                PersistenceTestFixture.STORY_ID,
                PersistenceTestFixture.EXECUTION_ID,
                PersistenceTestFixture.ACTION_ID,
                ExternalActionResultCategory.SUCCESS);
    }
}
