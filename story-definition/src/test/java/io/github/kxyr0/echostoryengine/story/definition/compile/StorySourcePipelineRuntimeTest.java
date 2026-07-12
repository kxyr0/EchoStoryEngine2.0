package io.github.kxyr0.echostoryengine.story.definition.compile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.core.application.StoryRuntimeUseCase;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.definition.SourcePipelineFixture;
import org.junit.jupiter.api.Test;

final class StorySourcePipelineRuntimeTest {
    @Test
    void validSourceCompilesIntoRuntimeRepresentation() {
        StoryCompilationResult result = new StorySourcePipeline().compile(
                SourcePipelineFixture.validInput(),
                SourcePipelineFixture.TIME);

        assertTrue(result.parsingSucceeded());
        assertTrue(result.structuralValidationSucceeded());
        assertTrue(result.semanticValidationSucceeded());
        assertTrue(result.compilationSucceeded());
        assertTrue(result.compiledStory().isPresent());
    }

    @Test
    void compiledStoryExecutesThroughRuntimeToWaitingAndFinished() {
        StoryDefinition story = compiledValidStory();
        SourcePipelineFixture.RecordingRepository repository = new SourcePipelineFixture.RecordingRepository();
        StoryRuntimeUseCase useCase = SourcePipelineFixture.useCase(story, repository);

        StoryRuntimeResult waiting = useCase.advance(SourcePipelineFixture.startInput());
        StoryRuntimeResult finished = useCase.advance(SourcePipelineFixture.successInput());

        assertEquals(StoryExecutionPhase.WAITING, waiting.state().phase());
        assertEquals(SourcePipelineFixture.CONFIRM_NODE, waiting.state().currentNode());
        assertEquals(SourcePipelineFixture.ACTION_ID, waiting.commands().get(0).actionExecutionId());
        assertEquals("ready", waiting.commands().get(0).payload().get("message"));
        assertEquals(StoryExecutionPhase.FINISHED, finished.state().phase());
        assertTrue(finished.state().hasCompleted(SourcePipelineFixture.ACTION_ID));
    }

    @Test
    void invalidSourceNeverProducesCompiledOutput() {
        String invalid = SourcePipelineFixture.validInput().content()
                .replace("\"next\": \"finish\"", "\"next\": \"missing\"");

        StoryCompilationResult result = SourcePipelineFixture.compile(invalid);

        assertFalse(result.compilationSucceeded());
        assertTrue(result.compiledStory().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("ESE-SOURCE-007")));
    }

    private static StoryDefinition compiledValidStory() {
        return new StorySourcePipeline()
                .compile(SourcePipelineFixture.validInput(), SourcePipelineFixture.TIME)
                .compiledStory()
                .orElseThrow();
    }
}
