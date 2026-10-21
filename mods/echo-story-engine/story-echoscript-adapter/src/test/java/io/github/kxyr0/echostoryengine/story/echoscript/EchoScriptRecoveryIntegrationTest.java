package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadStatus;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.persistence.file.FileExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.persistence.recovery.ExecutionRecoveryPlanner;
import io.github.kxyr0.echostoryengine.story.persistence.recovery.RecoveryResult;
import io.github.kxyr0.echostoryengine.story.persistence.recovery.RecoveryStatus;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class EchoScriptRecoveryIntegrationTest {
    @TempDir
    private Path root;

    @Test
    void minimalStoryRecoversWaitingActionAndFinishesWithFreshObjects() {
        StoryDefinition story = EchoScriptAdapterFixture.compiledMinimalStory();
        StoryRuntimeResult waiting = runtime(story, repository()).advance(EchoScriptAdapterFixture.startInput());

        FileExecutionStateRepository freshRepository = repository();
        ExecutionLoadResult loaded = freshRepository.load(EchoScriptAdapterFixture.EXECUTION_ID);
        RecoveryResult recovered = recover(loaded, equivalentStory());
        StoryRuntimeResult finished = runtime(equivalentStory(), repository())
                .advance(EchoScriptAdapterFixture.successInput());
        RecoveryResult terminal = recover(repository().load(EchoScriptAdapterFixture.EXECUTION_ID), equivalentStory());

        assertEquals(StoryExecutionPhase.WAITING, waiting.state().phase());
        assertEquals("true", waiting.state().variables().get("sample.ready"));
        assertEquals(ExecutionLoadStatus.FOUND, loaded.status());
        assertEquals(EchoScriptAdapterFixture.ACTION_ID, waiting.commands().get(0).actionExecutionId());
        assertEquals(RecoveryStatus.RECOVERY_REQUESTED, recovered.status());
        assertEquals(EchoScriptAdapterFixture.ACTION_ID, recovered.commands().get(0).actionExecutionId());
        assertEquals(StoryExecutionPhase.FINISHED, finished.state().phase());
        assertTrue(repository().load(EchoScriptAdapterFixture.EXECUTION_ID)
                .checkpoint()
                .orElseThrow()
                .pendingAction()
                .isEmpty());
        assertEquals(RecoveryStatus.TERMINAL, terminal.status());
        assertTrue(terminal.commands().isEmpty());
    }

    @Test
    void incompatibleSemanticChangeRejectsWaitingRestore() {
        StoryDefinition story = EchoScriptAdapterFixture.compiledMinimalStory();
        runtime(story, repository()).advance(EchoScriptAdapterFixture.startInput());

        StoryDefinition incompatible = EchoScriptAdapterFixture.compile(
                "fixtures/minimal_story.es",
                EchoScriptAdapterFixture.minimalStory().replace("sample:confirm", "sample:changed"))
                .compiledStory()
                .orElseThrow();
        RecoveryResult recovered = recover(repository().load(EchoScriptAdapterFixture.EXECUTION_ID), incompatible);

        assertEquals(RecoveryStatus.REJECTED, recovered.status());
        assertEquals("ESE-PERSISTENCE-006", recovered.diagnostics().get(0).code());
    }

    private RecoveryResult recover(ExecutionLoadResult loaded, StoryDefinition story) {
        assertEquals(ExecutionLoadStatus.FOUND, loaded.status());
        return new ExecutionRecoveryPlanner().recover(
                loaded.checkpoint().orElseThrow(),
                storyId -> Optional.of(story),
                EchoScriptAdapterFixture.TIME);
    }

    private io.github.kxyr0.echostoryengine.story.core.application.StoryRuntimeUseCase runtime(
            StoryDefinition story,
            FileExecutionStateRepository repository) {
        return EchoScriptAdapterFixture.runtime(story, repository);
    }

    private FileExecutionStateRepository repository() {
        StoryClock clock = () -> EchoScriptAdapterFixture.TIME;
        return new FileExecutionStateRepository(root, clock);
    }

    private StoryDefinition equivalentStory() {
        String source = EchoScriptAdapterFixture.minimalStory()
                .replace("class MinimalStory", "// equivalent source\nclass MinimalStory");
        return EchoScriptAdapterFixture.compile("different/source.es", source).compiledStory().orElseThrow();
    }
}
