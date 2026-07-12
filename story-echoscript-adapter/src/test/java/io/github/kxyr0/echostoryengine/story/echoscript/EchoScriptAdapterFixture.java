package io.github.kxyr0.echostoryengine.story.echoscript;

import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.application.StoryRuntimeUseCase;
import io.github.kxyr0.echostoryengine.story.core.domain.RuntimeTransitionPolicy;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Set;

final class EchoScriptAdapterFixture {
    static final Instant TIME = Instant.parse("2026-07-12T00:00:00Z");
    static final StoryId STORY_ID = StoryId.of("sample:hello_story");
    static final ExecutionId EXECUTION_ID = ExecutionId.of("sample:execution");
    static final NodeId START_NODE = NodeId.of("start");
    static final NodeId ACTION_NODE = NodeId.of("confirm");
    static final NodeId FINISH_NODE = NodeId.of("finish");
    static final NamespacedId ACTION_TYPE = NamespacedId.of("sample:confirm");
    static final ActionExecutionId ACTION_ID = ActionExecutionId.of("sample:execution/confirm");

    private EchoScriptAdapterFixture() {
    }

    static StoryCompilationResult compile(String sourceId, String source) {
        return new EchoScriptStorySourceCompiler().compile(new StorySourceInput(sourceId, source), TIME);
    }

    static StoryDefinition compiledMinimalStory() {
        return compile("fixtures/minimal_story.es", minimalStory()).compiledStory().orElseThrow();
    }

    static StoryRuntimeUseCase runtime(StoryDefinition story, ExecutionStateRepository repository) {
        RuntimeTransitionPolicy policy = new RuntimeTransitionPolicy(story, Set.of(ACTION_TYPE), 16);
        StoryClock clock = () -> TIME;
        return new StoryRuntimeUseCase(repository, diagnostic -> { }, clock, policy);
    }

    static StoryRuntimeInput startInput() {
        return StoryRuntimeInput.start(STORY_ID, EXECUTION_ID);
    }

    static StoryRuntimeInput successInput() {
        return StoryRuntimeInput.externalActionCompleted(
                STORY_ID,
                EXECUTION_ID,
                ACTION_ID,
                ExternalActionResultCategory.SUCCESS);
    }

    static StoryRuntimeInput unknownCompletionInput() {
        return StoryRuntimeInput.externalActionCompleted(
                STORY_ID,
                EXECUTION_ID,
                ActionExecutionId.of("sample:execution/unknown"),
                ExternalActionResultCategory.SUCCESS);
    }

    static String minimalStory() {
        return resource("/fixtures/minimal_story.es");
    }

    static String dialogueStory() {
        return resource("/fixtures/chapter_1_home.es");
    }

    private static String resource(String path) {
        try (var stream = EchoScriptAdapterFixture.class.getResourceAsStream(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
