package io.github.kxyr0.echostoryengine.story.forge.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionState;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.extension.catalog.ExtensionCatalog;
import io.github.kxyr0.echostoryengine.story.forge.extension.dispatch.ExtensionCompletionQueue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class StoryStartCommandSupportTest {
    private static final StoryId PARENT_STORY = StoryId.of("jdh:parent_story");
    private static final StoryId CHILD_STORY = StoryId.of("jdh:child_story");
    private static final ExecutionId PARENT_EXECUTION = ExecutionId.of("jdh:parent_execution");
    private static final ActionExecutionId ACTION_ID = ActionExecutionId.of("jdh:parent_execution/load_story");

    private final StoryStartCommandSupport support = new StoryStartCommandSupport();

    @Test
    void startsExistingStoryWithDerivedExecutionId() {
        AtomicReference<StoryId> startedStory = new AtomicReference<>();
        AtomicReference<ExecutionId> startedExecution = new AtomicReference<>();

        ExternalActionResultCategory result = support.apply(new StoryStartCommandSupport.StoryStartCommandContext(
                StoryExecutionState.start(PARENT_STORY, PARENT_EXECUTION),
                command(Map.of("storyId", CHILD_STORY.asString())),
                CHILD_STORY::equals,
                (storyId, executionId) -> {
                    startedStory.set(storyId);
                    startedExecution.set(executionId);
                }));

        assertEquals(ExternalActionResultCategory.SUCCESS, result);
        assertEquals(CHILD_STORY, startedStory.get());
        assertEquals(ExecutionId.of("jdh:parent_execution/load_story/story"), startedExecution.get());
    }

    @Test
    void rejectsMissingStory() {
        AtomicReference<StoryId> startedStory = new AtomicReference<>();

        ExternalActionResultCategory result = support.apply(new StoryStartCommandSupport.StoryStartCommandContext(
                StoryExecutionState.start(PARENT_STORY, PARENT_EXECUTION),
                command(Map.of("storyId", CHILD_STORY.asString())),
                storyId -> false,
                (storyId, executionId) -> startedStory.set(storyId)));

        assertEquals(ExternalActionResultCategory.FAILURE, result);
        assertTrue(startedStory.get() == null);
    }

    @Test
    void commandSinkStartsStoryAndCompletesParentAction() {
        ExtensionCompletionQueue completions = new ExtensionCompletionQueue(4);
        AtomicReference<StoryId> startedStory = new AtomicReference<>();
        AtomicReference<ExecutionId> startedExecution = new AtomicReference<>();
        ForgeCommandSinkWiring wiring = new ForgeCommandSinkWiring(
                new EnvironmentCatalogState(Instant.EPOCH),
                diagnostic -> { },
                () -> Instant.EPOCH,
                new ForgeRuntimeDiagnostics());

        wiring.create(
                null,
                ExtensionCatalog.empty(),
                completions,
                new ForgeCommandSinkWiring.StoryLaunchWiring(
                        CHILD_STORY::equals,
                        (storyId, executionId) -> {
                            startedStory.set(storyId);
                            startedExecution.set(executionId);
                        }))
                .dispatch(waitingState(), command(Map.of("storyId", CHILD_STORY.asString())));

        List<ActionCompletion> drained = new ArrayList<>();
        assertEquals(1, completions.drain(4, drained::add));
        assertEquals(CHILD_STORY, startedStory.get());
        assertEquals(ExecutionId.of("jdh:parent_execution/load_story/story"), startedExecution.get());
        assertEquals(ExternalActionResultCategory.SUCCESS, drained.get(0).resultCategory());
        assertEquals(ACTION_ID, drained.get(0).actionExecutionId());
    }

    private static ExternalCommand command(Map<String, String> payload) {
        return new ExternalCommand(StoryStartCommandSupport.ACTION_TYPE, ACTION_ID, payload);
    }

    private static StoryExecutionState waitingState() {
        return StoryExecutionState.start(PARENT_STORY, PARENT_EXECUTION)
                .waitingAt(NodeId.of("load_story"), ACTION_ID);
    }
}
