package io.github.kxyr0.echostoryengine.story.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class StoryRuntimeOutputBindingTest {
    @Test
    void genericOutputBindsAndPersistsAsStoryVariable() {
        RuntimeTestFixture.RecordingRepository repository = new RuntimeTestFixture.RecordingRepository();
        StoryRuntimeUseCase useCase = useCase(repository);

        useCase.advance(RuntimeTestFixture.startInput());
        StoryRuntimeResult result = useCase.advance(RuntimeTestFixture.successfulCompletion(
                Map.of(RuntimeTestFixture.OUTPUT_NAME, "accepted")));

        assertEquals(StoryExecutionPhase.FINISHED, result.state().phase());
        assertEquals("accepted", result.state().variables().get(RuntimeTestFixture.OUTPUT_NAME));
        assertEquals("accepted", repository
                .state(RuntimeTestFixture.EXECUTION_ID)
                .variables()
                .get(RuntimeTestFixture.OUTPUT_NAME));
    }

    @Test
    void missingBoundOutputFailsBeforeBranching() {
        StoryRuntimeUseCase useCase = useCase(new RuntimeTestFixture.RecordingRepository());

        useCase.advance(RuntimeTestFixture.startInput());
        StoryRuntimeResult result = useCase.advance(RuntimeTestFixture.successfulCompletion());

        assertEquals(StoryExecutionPhase.FAILED, result.state().phase());
        assertEquals("ESE-ACTION-006", result.state().failureCode());
        assertEquals("ESE-ACTION-006", result.diagnostics().get(0).code());
    }

    @Test
    void mismatchedBoundOutputUsesGenericConditionFailure() {
        StoryRuntimeUseCase useCase = useCase(new RuntimeTestFixture.RecordingRepository());

        useCase.advance(RuntimeTestFixture.startInput());
        StoryRuntimeResult result = useCase.advance(RuntimeTestFixture.successfulCompletion(
                Map.of(RuntimeTestFixture.OUTPUT_NAME, "refused")));

        assertEquals(StoryExecutionPhase.FAILED, result.state().phase());
        assertEquals("refused", result.state().variables().get(RuntimeTestFixture.OUTPUT_NAME));
        assertEquals("ESE-RUNTIME-003", result.state().failureCode());
    }

    @Test
    void duplicateCompletionCannotRebindOutput() {
        StoryRuntimeUseCase useCase = useCase(new RuntimeTestFixture.RecordingRepository());

        useCase.advance(RuntimeTestFixture.startInput());
        StoryRuntimeResult finished = useCase.advance(RuntimeTestFixture.successfulCompletion(
                Map.of(RuntimeTestFixture.OUTPUT_NAME, "accepted")));
        StoryRuntimeResult duplicate = useCase.advance(RuntimeTestFixture.successfulCompletion(
                Map.of(RuntimeTestFixture.OUTPUT_NAME, "changed")));

        assertEquals(finished.state(), duplicate.state());
        assertEquals("accepted", duplicate.state().variables().get(RuntimeTestFixture.OUTPUT_NAME));
        assertEquals("ESE-ACTION-003", duplicate.diagnostics().get(0).code());
    }

    private StoryRuntimeUseCase useCase(RuntimeTestFixture.RecordingRepository repository) {
        return RuntimeTestFixture.useCase(
                repository,
                new RuntimeTestFixture.RecordingSink(),
                RuntimeTestFixture.outputBindingStory(),
                Set.of(RuntimeTestFixture.ACTION_TYPE));
    }
}
