package io.github.kxyr0.echostoryengine.story.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class StoryRuntimeUseCaseTest {
    @Test
    void completeHappyPathStoryReachesFinished() {
        RuntimeTestFixture.RecordingRepository repository = new RuntimeTestFixture.RecordingRepository();
        StoryRuntimeUseCase useCase = RuntimeTestFixture.useCase(repository, new RuntimeTestFixture.RecordingSink());

        useCase.advance(RuntimeTestFixture.startInput());
        StoryRuntimeResult result = useCase.advance(RuntimeTestFixture.successfulCompletion());

        assertEquals(StoryExecutionPhase.FINISHED, result.state().phase());
        assertEquals(RuntimeTestFixture.FINISH_NODE, result.state().currentNode());
        assertEquals("ready", result.state().variables().get("hello.flag"));
        assertTrue(result.state().hasCompleted(RuntimeTestFixture.ACTION_ID));
    }

    @Test
    void runtimeEntersWaitingBeforeCompletion() {
        StoryRuntimeUseCase useCase = RuntimeTestFixture.useCase(
                new RuntimeTestFixture.RecordingRepository(),
                new RuntimeTestFixture.RecordingSink());

        StoryRuntimeResult result = useCase.advance(RuntimeTestFixture.startInput());

        assertEquals(StoryExecutionPhase.WAITING, result.state().phase());
        assertEquals(RuntimeTestFixture.ACTION_NODE, result.state().currentNode());
        assertEquals(RuntimeTestFixture.ACTION_ID.asString(), result.state().pendingActionExecutionId());
        assertEquals(RuntimeTestFixture.ACTION_TYPE, result.commands().get(0).actionType());
        assertEquals(RuntimeTestFixture.ACTION_ID, result.commands().get(0).actionExecutionId());
    }

    @Test
    void successfulCompletionResumesTheCorrectExecution() {
        RuntimeTestFixture.RecordingRepository repository = new RuntimeTestFixture.RecordingRepository();
        StoryRuntimeUseCase useCase = RuntimeTestFixture.useCase(repository, new RuntimeTestFixture.RecordingSink());
        ExecutionId secondExecution = ExecutionId.of("echo_story_engine:second-execution");

        useCase.advance(RuntimeTestFixture.startInput());
        useCase.advance(RuntimeTestFixture.startInput(secondExecution));
        StoryRuntimeResult firstResult = useCase.advance(RuntimeTestFixture.successfulCompletion());

        assertEquals(StoryExecutionPhase.FINISHED, firstResult.state().phase());
        assertEquals(StoryExecutionPhase.WAITING, repository.load(secondExecution).orElseThrow().phase());
    }

    @Test
    void duplicateCompletionDoesNotExecuteTheNextStepTwice() {
        StoryRuntimeUseCase useCase = RuntimeTestFixture.useCase(
                new RuntimeTestFixture.RecordingRepository(),
                new RuntimeTestFixture.RecordingSink());

        useCase.advance(RuntimeTestFixture.startInput());
        StoryRuntimeResult finished = useCase.advance(RuntimeTestFixture.successfulCompletion());
        StoryRuntimeResult duplicate = useCase.advance(RuntimeTestFixture.successfulCompletion());

        assertEquals(finished.state(), duplicate.state());
        assertEquals(0, duplicate.commands().size());
        assertEquals("ESE-ACTION-003", duplicate.diagnostics().get(0).code());
    }

    @Test
    void unknownCompletionIdIsHandledSafely() {
        StoryRuntimeUseCase useCase = RuntimeTestFixture.useCase(
                new RuntimeTestFixture.RecordingRepository(),
                new RuntimeTestFixture.RecordingSink());
        ActionExecutionId unknownId = ActionExecutionId.of("echo_story_engine:unknown-action");

        StoryRuntimeResult waiting = useCase.advance(RuntimeTestFixture.startInput());
        StoryRuntimeResult unknown = useCase.advance(RuntimeTestFixture.completion(
                RuntimeTestFixture.EXECUTION_ID,
                unknownId,
                ExternalActionResultCategory.SUCCESS));

        assertEquals(waiting.state(), unknown.state());
        assertEquals("ESE-ACTION-004", unknown.diagnostics().get(0).code());
    }

    @Test
    void missingActionSupportProducesDiagnosticAndFailsOnlyThatExecution() {
        RuntimeTestFixture.RecordingRepository repository = new RuntimeTestFixture.RecordingRepository();
        StoryRuntimeUseCase useCase = RuntimeTestFixture.useCase(
                repository,
                new RuntimeTestFixture.RecordingSink(),
                RuntimeTestFixture.helloStory(),
                Set.of());

        StoryRuntimeResult result = useCase.advance(RuntimeTestFixture.startInput());

        assertEquals(StoryExecutionPhase.FAILED, result.state().phase());
        assertEquals("ESE-ACTION-001", result.state().failureCode());
        assertEquals("ESE-ACTION-001", result.diagnostics().get(0).code());
        assertEquals(0, result.commands().size());
    }
}
